package rest;

import DTO.PersonDTO;
import DTO.PersonsDTO;
import entities.Address;
import entities.Person;
import io.restassured.RestAssured;
import static io.restassured.RestAssured.given;
import io.restassured.http.ContentType;
import io.restassured.parsing.Parser;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.EMF_Creator;

/**
 *
 * @author Nicklas Nielsen
 */
public class PersonResourceTest {

    private static final int SERVER_PORT = 7777;
    private static final String SERVER_URL = "http://localhost/api/person";
    private static List<Person> persons;
    private static List<PersonDTO> personDTOs;
    private static PersonsDTO personsDTO;
    private static final URI BASE_URI = UriBuilder.fromUri(SERVER_URL).port(SERVER_PORT).build();
    private static HttpServer httpServer;
    private static EntityManagerFactory emf;

    static HttpServer startServer() {
        ResourceConfig rc = ResourceConfig.forApplication(new ApplicationConfig());
        return GrizzlyHttpServerFactory.createHttpServer(BASE_URI, rc);
    }

    @BeforeAll
    public static void setUpClass() {
        EMF_Creator.startREST_TestWithDB();
        emf = EMF_Creator.createEntityManagerFactoryForTest();
        persons = new ArrayList<>();
        personDTOs = new ArrayList<>();
        httpServer = startServer();
        RestAssured.baseURI = SERVER_URL;
        RestAssured.port = SERVER_PORT;
        RestAssured.defaultParser = Parser.JSON;
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createNamedQuery("Person.deleteAllRows").executeUpdate();
            em.createNamedQuery("Address.deleteAllRows").executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    @AfterAll
    public static void tearDownClass() {
        EMF_Creator.endREST_TestWithDB();
        httpServer.shutdownNow();
    }

    @BeforeEach
    public void setUp() {
        EntityManager em = emf.createEntityManager();
        // Add test data here
        Address a1 = new Address("Sverige", 3555, "Sverigeborg");
        Address a2 = new Address("Afrika", 3555, "Detroit");
        Address a3 = new Address("Danmark", 3555, "Himlen");
        persons.add(new Person("Nicklas", "Nielsen", "11111111"));
        persons.add(new Person("Mathias", "Nielsen", "22222222"));
        persons.add(new Person("Nikolaj", "Larsen", "11223344"));
        persons.get(0).setAddress(a1);
        persons.get(1).setAddress(a2);
        persons.get(2).setAddress(a3);
        
        try {
            em.getTransaction().begin();
            for (Person person : persons) {
                em.persist(person);
                em.flush();
                em.clear();
            }
            em.getTransaction().commit();
        } finally {
            em.close();
        }
        persons.forEach(person -> {
            personDTOs.add(new PersonDTO(person));
        });
        personsDTO = new PersonsDTO(persons);
    }

    @AfterEach
    public void tearDown() {
        persons.clear();
        personDTOs.clear();
        personsDTO = null;
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.createNamedQuery("Person.deleteAllRows").executeUpdate();
            em.createNamedQuery("Address.deleteAllRows").executeUpdate();
            em.getTransaction().commit();
        } finally {
            em.close();
        }
    }

    @Test
    public void testAPI_online() {
        given().when().get("/person").then().statusCode(200);
    }
    
    @Test
    public void testEditPerson_server_error() {
        given().when().put("/person/id/1").then().statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500.getStatusCode());
    }

    @Test
    public void testAddPerson_server_error() {
        given().contentType(ContentType.JSON).when().post("/person").then().statusCode(HttpStatus.INTERNAL_SERVER_ERROR_500.getStatusCode());
    }
    
    @Test
    public void testGetAllPersons_size() {
        given()
                .contentType(ContentType.JSON)
                .get("/person/all")
                .then()
                .assertThat()
                .body("all.size()", is(personDTOs.size()));
    }

    @Test
    public void testGetAllPersons_content() {
        List<PersonDTO> expected = personDTOs;
        
        List<PersonDTO> actual = given()
                .contentType(ContentType.JSON)
                .when()
                .get("/person/all")
                .then()
                .extract().body().jsonPath().getList("all", PersonDTO.class);
    }

    @Test
    public void testGetPersonById_found() {
        PersonDTO expected = personDTOs.get(0);
        PersonDTO actual = given()
                .when()
                .get("/person/id/" + expected.getId())
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().body().as(PersonDTO.class);
        assertThat(actual, is(expected));
    }

    @Test
    public void testGetPersonById_not_found() {
        int id = personDTOs.get(personDTOs.size() - 1).getId() + 1;
        given().when().get("/person/id/" + id).then().statusCode(404);
    }

    @Test
    public void testAddPerson_added() {
        Address a1 = new Address("Himlen", 6969, "Danmark");
        Person person = new Person("Sven", "Den Almægtige", "26354725");
        person.setAddress(a1);
        PersonDTO expected = new PersonDTO(person);
        
        PersonDTO actual = given()
                .contentType(ContentType.JSON)
                .body(expected)
                .when()
                .post("/person")
                .then()
                .statusCode(Response.Status.OK.getStatusCode())
                .extract().body().as(PersonDTO.class);
        assertThat(actual, is(expected));
    }

    @Test
    public void testAddPerson_invalid_FirstName() {
        Address a1 = new Address("Himlen", 6969, "Danmark");
        Person person = new Person("", "Den Almægtige", "272466754");
        person.setAddress(a1);
        PersonDTO personDTO = new PersonDTO(person);
        
        given()
                .contentType(ContentType.JSON)
                .body(personDTO)
                .post("/person")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST_400.getStatusCode());
    }
    
    @Test
    public void testAddPerson_invalid_LastName() {
        Address a1 = new Address("Himlen", 6969, "Danmark");
        Person person = new Person("Sven", "", "272466754");
        person.setAddress(a1);
        PersonDTO personDTO = new PersonDTO(person);
        
        given()
                .contentType(ContentType.JSON)
                .body(personDTO)
                .post("/person")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST_400.getStatusCode());
    }

    @Test
    public void testEditPerson_edited() {
        PersonDTO personDTO = personDTOs.get(0);
        personDTO.setFirstName("Sven");
        
        given()
                .contentType(ContentType.JSON)
                .body(personDTO)
                .when()
                .put("/person/id/" + personDTO.getId())
                .then()
                .statusCode(200);
    }

    @Test
    public void testEditPerson_invalid_id() {
        PersonDTO personDTO = personDTOs.get(personDTOs.size() - 1);
        personDTO.setId(personDTO.getId() + 1);
        given()
                .contentType(ContentType.JSON)
                .body(personDTO)
                .when()
                .put("/person/id" + personDTO.getId())
                .then()
                .statusCode(404);
    }

    @Test
    public void testEditPerson_invalid_FirstName() {
        PersonDTO personDTO = personDTOs.get(0);
        personDTO.setFirstName("");
        
        given()
                .contentType(ContentType.JSON)
                .body(personDTO)
                .put("/person/id/" + personDTO.getId())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST_400.getStatusCode());
    }
    
    @Test
    public void testEditPerson_invalid_LastName() {
        PersonDTO personDTO = personDTOs.get(0);
        personDTO.setLastName("");
        
        given()
                .contentType(ContentType.JSON)
                .body(personDTO)
                .put("/person/id/" + personDTO.getId())
                .then()
                .statusCode(HttpStatus.BAD_REQUEST_400.getStatusCode());
    }

    @Test
    public void testDeletePerson_deleted() {
        int id = personDTOs.get(0).getId();
        given().when().delete("/person/id/" + id).then().statusCode(200);
    }

    @Test
    public void testDeletePerson_invalid_id() {
        int id = personDTOs.get(personDTOs.size() - 1).getId() + 1;
        given().when().delete("/person/id/" + id).then().statusCode(404);
    }
}
