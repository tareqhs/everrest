import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("a")
class CroovyResource3
{
   
   CroovyResource3()
   {
   }
   
   @GET
   @Path("b")
   def m0()
   {
      return System.getProperty("java.home"); 
   }
   
}