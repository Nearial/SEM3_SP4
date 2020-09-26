package facades;

import DTO.PersonDTO;
import DTO.PersonsDTO;
import entities.Person;
import Exceptions.MissingInputException;
import Exceptions.PersonNotFoundException;
import entities.Address;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import utils.EMF_Creator;

/**
 *
 * @author Nikolaj Larsen
 */
public class PersonFacadeTest {

    private static EntityManagerFactory emf;
    private static PersonFacade facade;
    private static List<Person> persons;
    private static List<PersonDTO> personDTOs;
    private static PersonsDTO personsDTO;

    @BeforeAll
    public static void setUpClass() {
        emf = EMF_Creator.createEntityManagerFactoryForTest();
        facade = PersonFacade.getPersonFacade(emf);
        persons = new ArrayList<>();
        personDTOs = new ArrayList<>();
        
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
    public void testAddPerson_success() throws MissingInputException {
        // Arrange
        PersonDTO expected = personDTOs.get(0);

        // Act
        PersonDTO actual = facade.addPerson(expected.getFirstName(),
                expected.getLastName(), expected.getPhone(),
                expected.getStreet(), expected.getZip(),
                expected.getCity());

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void testAddPerson_invalid_firstName() throws MissingInputException {
        // Arrange
        PersonDTO person = personDTOs.get(0);
        person.setFirstName("");

        // Act
        MissingInputException exception = assertThrows(MissingInputException.class, ()
                -> facade.addPerson(person.getFirstName(),
                person.getLastName(), person.getPhone(),
                person.getStreet(), person.getZip(),
                person.getCity())
        );

        // Assert
        assertTrue(exception.getMessage().equals("First and / or Last Name is missing"));
    }

    @Test
    public void testAddPerson_invalid_lastName() throws MissingInputException {
        // Arrange
        PersonDTO person = personDTOs.get(0);
        person.setLastName("");

        // Act
        MissingInputException exception = assertThrows(MissingInputException.class, ()
                -> facade.addPerson(person.getFirstName(),
                person.getLastName(), person.getPhone(),
                person.getStreet(), person.getZip(),
                person.getCity())
        );

        // Assert
        assertTrue(exception.getMessage().equals("First and / or Last Name is missing"));
    }

    @Test
    public void testAddPerson_invalid_firstName_and_lastName() throws MissingInputException {
        // Arrange
        PersonDTO person = personDTOs.get(0);
        person.setFirstName("");
        person.setLastName("");

        // Act
        MissingInputException exception = assertThrows(MissingInputException.class, ()
                -> facade.addPerson(person.getFirstName(),
                person.getLastName(), person.getPhone(),
                person.getStreet(), person.getZip(),
                person.getCity())
        );

        // Assert
        assertTrue(exception.getMessage().equals("First and / or Last Name is missing"));
    }

    @Test
    public void testDeletePerson_success() throws PersonNotFoundException {
        // Arrange
        PersonDTO expected = personDTOs.get(0);

        // Act
        PersonDTO actual = facade.deletePerson(expected.getId());

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void testDeletePerson_invalid_id() throws PersonNotFoundException {
        // Arrange
        PersonDTO person = personDTOs.get(personDTOs.size() - 1);
        person.setId(person.getId() + 1);

        // Act
        PersonNotFoundException exception = assertThrows(PersonNotFoundException.class, ()
                -> facade.deletePerson(person.getId())
        );

        // Assert
        assertTrue(exception.getMessage().equals("Could not delete, provided id does not exist"));
    }

    @Test
    public void testGetPerson_success() throws PersonNotFoundException {
        // Arrange
        PersonDTO expected = personDTOs.get(0);

        // Act
        PersonDTO actual = facade.getPerson(expected.getId());

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void testGetPerson_invalid_id() throws PersonNotFoundException {
        // Arrange
        PersonDTO person = personDTOs.get(personDTOs.size() - 1);
        person.setId(person.getId() + 1);

        // Act
        PersonNotFoundException exception = assertThrows(PersonNotFoundException.class, ()
                -> facade.getPerson(person.getId())
        );

        // Assert
        assertTrue(exception.getMessage().equals("No person with provided id found"));
    }

    @Test
    public void testGetAllPersons_success() {
        // Arrange
        PersonsDTO expected = personsDTO;

        // Act
        PersonsDTO actual = facade.getAllPersons();

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void testEditPerson_success() throws PersonNotFoundException, MissingInputException {
        // Arrange
        PersonDTO expected = personDTOs.get(0);
        expected.setFirstName("Lars");
        expected.setLastName("Larsen");
        expected.setPhone("00000000");

        // Act
        PersonDTO actual = facade.editPerson(expected);

        // Assert
        assertEquals(expected, actual);
    }

    @Test
    public void testEditPerson_invalid_firstName() throws PersonNotFoundException, MissingInputException {
        // Arrange
        PersonDTO person = personDTOs.get(0);
        person.setFirstName("");

        // Act
        MissingInputException exception = assertThrows(MissingInputException.class, ()
                -> facade.editPerson(person)
        );

        // Assert
        assertTrue(exception.getMessage().equals("First and / or Last Name is missing"));
    }

    @Test
    public void testEditPerson_invalid_lastName() throws PersonNotFoundException, MissingInputException {
        // Arrange
        PersonDTO person = personDTOs.get(0);
        person.setLastName("");

        // Act
        MissingInputException exception = assertThrows(MissingInputException.class, ()
                -> facade.editPerson(person)
        );

        // Assert
        assertTrue(exception.getMessage().equals("First and / or Last Name is missing"));
    }

    @Test
    public void testEditPerson_invalid_firstName_and_lastName() throws PersonNotFoundException, MissingInputException {
        // Arrange
        PersonDTO person = personDTOs.get(0);
        person.setFirstName("");
        person.setLastName("");

        // Act
        MissingInputException exception = assertThrows(MissingInputException.class, ()
                -> facade.editPerson(person)
        );

        // Assert
        assertTrue(exception.getMessage().equals("First and / or Last Name is missing"));
    }

    @Test
    public void testEditPerson_invalid_id() throws PersonNotFoundException, MissingInputException {
        // Arrange
        PersonDTO person = personDTOs.get(personDTOs.size() - 1);
        person.setId(person.getId() + 1);

        // Act
        PersonNotFoundException exception = assertThrows(PersonNotFoundException.class, ()
                -> facade.editPerson(person)
        );

        // Assert
        assertTrue(exception.getMessage().equals("Could not edit, provided id does not exist"));
    }
}