/*
 * Copyright (C) 2012 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.everrest.websockets;

import org.everrest.core.impl.ContainerRequest;
import org.everrest.core.impl.ContainerResponse;
import org.everrest.core.impl.EnvironmentContext;
import org.everrest.core.impl.EverrestProcessor;
import org.everrest.core.impl.InputHeadersMap;
import org.everrest.core.impl.async.AsynchronousJob;
import org.everrest.core.impl.async.AsynchronousJobListener;
import org.everrest.core.impl.async.AsynchronousJobPool;
import org.everrest.core.tools.SecurityContextRequest;
import org.everrest.core.util.Logger;
import org.everrest.websockets.message.InputMessage;
import org.everrest.websockets.message.MessageConversionException;
import org.everrest.websockets.message.OutputMessage;
import org.everrest.websockets.message.Pair;
import org.everrest.websockets.message.RESTfulInputMessage;
import org.everrest.websockets.message.RESTfulOutputMessage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.UUID;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;

/**
 * @author <a href="mailto:andrew00x@gmail.com">Andrey Parfonov</a>
 * @version $Id: $
 */
class WS2RESTAdapter implements WSMessageReceiver, WSConnectionListener
{
   private static final Logger LOG = Logger.getLogger(WS2RESTAdapter.class);

   private final SecurityContext securityContext;
   private final EverrestProcessor everrestProcessor;
   private final AsynchronousJobPool asynchronousPool;
   private WSConnection connection;

   WS2RESTAdapter(SecurityContext securityContext,
                  EverrestProcessor everrestProcessor,
                  AsynchronousJobPool asynchronousPool)
   {
      this.securityContext = securityContext;
      this.everrestProcessor = everrestProcessor;
      this.asynchronousPool = asynchronousPool;
   }

   //

   @Override
   public void onOpen(WSConnection connection)
   {
      this.connection = connection;
   }

   @Override
   public void onClose(Long connectionId, int status)
   {
      this.connection = null;
   }

   //

   @Override
   public void onMessage(InputMessage input)
   {
      // See method onTextMessage(CharBuffer) in class WSConnectionImpl.
      if (!(input instanceof RESTfulInputMessage))
      {
         throw new IllegalArgumentException("Invalid input message. ");
      }

      final RESTfulInputMessage restInputMessage = (RESTfulInputMessage)input;

      MultivaluedMap<String, String> headers = Pair.toMap(restInputMessage.getHeaders());

      if ("ping".equalsIgnoreCase(headers.getFirst("x-everrest-websocket-message-type")))
      {
         // At the moment is no JavaScript API to send ping message from client side.
         RESTfulOutputMessage pong = newOutputMessage(restInputMessage);
         // Copy body from ping request.
         pong.setBody(restInputMessage.getBody());
         pong.setResponseCode(200);
         pong.setHeaders(new Pair[]{new Pair("x-everrest-websocket-message-type", "pong")});
         safeSendMessage(pong);
         return;
      }

      final String internalUuid = UUID.randomUUID().toString();

      // This listener called when asynchronous task is done.
      asynchronousPool.registerListener(new AsynchronousJobListener()
      {
         @Override
         public void done(AsynchronousJob job)
         {
            MultivaluedMap<String, String> requestHeaders = ((ContainerRequest)job.getContext()
               .get("org.everrest.async.request")).getRequestHeaders();

            if ("websocket".equals(requestHeaders.getFirst("x-everrest-protocol"))
               && internalUuid.equals(requestHeaders.getFirst("x-everrest-websocket-tracker-id")))
            {
               URI requestUri = UriBuilder.fromPath("/async/" + job.getJobId()).build();
               ContainerRequest req = new SecurityContextRequest("GET", requestUri, URI.create(""), null,
                  new InputHeadersMap(), securityContext);

               RESTfulOutputMessage output = newOutputMessage(restInputMessage);
               ContainerResponse resp = new ContainerResponse(
                  new EverrestResponseWriter(output));

               try
               {
                  everrestProcessor.process(req, resp, new EnvironmentContext());
               }
               catch (Exception e)
               {
                  LOG.error(e.getMessage(), e);
               }
               finally
               {
                  // Not need this listener any more.
                  asynchronousPool.unregisterListener(this);
               }

               safeSendMessage(output);
            }
         }
      });

      ByteArrayInputStream data = null;
      String body = input.getBody();
      if (body != null)
      {
         try
         {
            data = new ByteArrayInputStream(body.getBytes("UTF-8"));
         }
         catch (UnsupportedEncodingException e)
         {
            // Should never happen since UTF-8 is supported.
            throw new IllegalStateException(e.getMessage(), e);
         }
      }

      URI requestUri = UriBuilder.fromPath("/").path(restInputMessage.getPath()).build();
      if (data != null)
      {
         // Always know content length since we use ByteArrayInputStream.
         headers.putSingle("content-length", Integer.toString(data.available()));
      }

      // Put some additional 'helper' headers.
      headers.putSingle("x-everrest-async", "true");
      headers.putSingle("x-everrest-protocol", "websocket");
      headers.putSingle("x-everrest-websocket-tracker-id", internalUuid);

      ContainerRequest req = new SecurityContextRequest(restInputMessage.getMethod(), requestUri, URI.create(""), data,
         new InputHeadersMap(headers), securityContext);

      RESTfulOutputMessage output = newOutputMessage(restInputMessage);
      ContainerResponse resp = new ContainerResponse(new EverrestResponseWriter(output));

      try
      {
         everrestProcessor.process(req, resp, new EnvironmentContext());
      }
      catch (Exception e)
      {
         LOG.error(e.getMessage(), e);
      }

      safeSendMessage(output);
   }

   @Override
   public void onError(Exception error)
   {
      LOG.error(error.getMessage(), error);
   }

   private RESTfulOutputMessage newOutputMessage(RESTfulInputMessage input)
   {
      RESTfulOutputMessage output = new RESTfulOutputMessage();
      output.setUuid(input.getUuid());
      output.setMethod(input.getMethod());
      output.setPath(input.getPath());
      return output;
   }

   private void safeSendMessage(OutputMessage output)
   {
      if (connection == null)
      {
         // Be sure we are able to write in output.
         throw new IllegalStateException("Connection is closed. ");
      }

      try
      {
         connection.sendMessage(output);
      }
      catch (MessageConversionException e)
      {
         LOG.error(e.getMessage(), e);
      }
      catch (IOException e)
      {
         LOG.error(e.getMessage(), e);
      }
   }
}