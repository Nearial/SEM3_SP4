package rest;

import DTO.ExceptionDTO;
import DTO.PersonDTO;
import Exceptions.MissingInputException;
import Exceptions.PersonNotFoundException;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import utils.EMF_Creator;
import facades.PersonFacade;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

//Todo Remove or change relevant parts before ACTUAL use
@Path("person")
public class PersonResource {

    private static final EntityManagerFactory EMF = EMF_Creator.createEntityManagerFactory();
    
    //An alternative way to get the EntityManagerFactory, whithout having to type the details all over the code
    //EMF = EMF_Creator.createEntityManagerFactory(DbSelector.DEV, Strategy.CREATE);
    
    private static final PersonFacade FACADE =  PersonFacade.getPersonFacade(EMF);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    
    @GET
    @Produces({MediaType.APPLICATION_JSON})
    public Response getDefault() {
        return Response.ok().build();
    }
            
    @GET
    @Path("all")
    @Produces({MediaType.APPLICATION_JSON})
    public String getAllPersons() {
        return GSON.toJson(FACADE.getAllPersons());
    }
    
    @GET
    @Path("id/{id}")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getPerson(@PathParam("id") int id) throws PersonNotFoundException{          
        return Response.ok(FACADE.getPerson(id)).build();
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    public Response addPerson(String person) throws MissingInputException{
        PersonDTO personDTO = GSON.fromJson(person, PersonDTO.class);
        PersonDTO personAdded = FACADE.addPerson(personDTO.getFirstName(),
                personDTO.getLastName(), personDTO.getPhone(),
                personDTO.getStreet(), personDTO.getZip(),
                personDTO.getCity());
        
        return Response.ok(personAdded).build();
    }
    
    @PUT
    @Consumes({MediaType.APPLICATION_JSON})
    @Produces({MediaType.APPLICATION_JSON})
    @Path("id/{id}")
    public Response editPerson(@PathParam("id") int id, String person)throws PersonNotFoundException, MissingInputException{
        PersonDTO personDTO = GSON.fromJson(person, PersonDTO.class);
        personDTO.setId(id);
        
        PersonDTO editedPerson = FACADE.editPerson(personDTO);
        
        return Response.ok(editedPerson).build();
    }
    
    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    @Path("id/{id}")
    public Response deletPerson(@PathParam("id") int id) throws PersonNotFoundException{
        FACADE.deletePerson(id);
        return Response.ok("{\"status\": \"removed\"}").build();
}
}
