package facades;

import DTO.PersonDTO;
import DTO.PersonsDTO;
import Exceptions.MissingInputException;
import Exceptions.PersonNotFoundException;
import entities.Address;
import entities.Person;
import java.util.Date;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

public class PersonFacade implements IPersonFacade {

    private static PersonFacade instance;
    private static EntityManagerFactory emf;

    //Private Constructor to ensure Singleton
    private PersonFacade() {
    }

    /**
     *
     * @param _emf
     * @return an instance of this facade class.
     */
    public static PersonFacade getPersonFacade(EntityManagerFactory _emf) {
        if (instance == null) {
            emf = _emf;
            instance = new PersonFacade();
        }
        return instance;
    }

    private EntityManager getEntityManager() {
        return emf.createEntityManager();
    }

    @Override
    public PersonDTO addPerson(String fName, String lName, String phone, String street, int zip, String city) throws MissingInputException {
        if (fName.isEmpty() || lName.isEmpty()) {
            throw new MissingInputException("First and / or Last Name is missing");
        } else if (street.isEmpty() || zip <= 0 || city.isEmpty()) {
            throw new MissingInputException("Street or City is missing");
        }

        EntityManager em = getEntityManager();

        Address address = getAddress(street, zip, city);
        Person person = new Person(fName, lName, phone);

        try {
            em.getTransaction().begin();
            em.persist(person);
            person.setAddress(address);
            em.merge(person);
            em.getTransaction().commit();

            return new PersonDTO(person);
        } finally {
            em.close();
        }
    }

    @Override
    public PersonDTO deletePerson(int id) throws PersonNotFoundException {
        EntityManager em = getEntityManager();

        try {
            Person person = em.find(Person.class, id);
            Address address = person.getAddress();

            if (person == null) {
                throw new PersonNotFoundException("Could not delete, provided id does not exist");
            }

            address.removePerson(person);

            boolean deleteAddress = false;
            if (address.getPersons().isEmpty()) {
                deleteAddress = true;
            }

            em.getTransaction().begin();
            em.remove(person);

            if (deleteAddress) {
                em.remove(address);
            }

            em.getTransaction().commit();

            return new PersonDTO(person);
        } finally {
            em.close();
        }
    }

    @Override
    public PersonDTO getPerson(int id) throws PersonNotFoundException {
        EntityManager em = getEntityManager();

        try {
            Person person = em.find(Person.class, id);

            if (person == null) {
                throw new PersonNotFoundException("No person with provided id found");
            }

            return new PersonDTO(person);
        } finally {
            em.close();
        }
    }

    @Override
    public PersonsDTO getAllPersons() {
        EntityManager em = getEntityManager();

        try {
            Query query = em.createNamedQuery("Persons.GetAll");
            List<Person> persons = query.getResultList();

            return new PersonsDTO(persons);
        } finally {
            em.close();
        }
    }

    @Override
    public PersonDTO editPerson(PersonDTO p) throws PersonNotFoundException, MissingInputException {
        EntityManager em = getEntityManager();

        if (p.getFirstName().isEmpty() || p.getLastName().isEmpty()) {
            throw new MissingInputException("First and / or Last Name is missing");
        }

        try {
            Person person = em.find(Person.class, p.getId());

            if (person == null) {
                throw new PersonNotFoundException("Could not edit, provided id does not exist");
            }

            person.setFirstName(p.getFirstName());
            person.setLastName(p.getLastName());
            person.setPhone(p.getPhone());
            person.getAddress().setStreet(p.getStreet());
            person.getAddress().setZip(p.getZip());
            person.getAddress().setCity(p.getCity());
            person.setLastEdited(new Date());

            em.getTransaction().begin();
            em.persist(person);
            em.getTransaction().commit();

            return new PersonDTO(person);
        } finally {
            em.close();
        }
    }

    private Address getAddress(String street, int zip, String city) {
        EntityManager em = getEntityManager();

        try {
            Query query = em.createNamedQuery("Address.getAddress");
            query.setParameter("street", street);
            query.setParameter("zip", zip);
            query.setParameter("city", city);
            Address address;

            List<Address> addresses = query.getResultList();

            if (addresses.isEmpty()) {
                address = new Address(street, zip, city);
            } else {
                address = addresses.get(0);
            }

            return address;
        } finally {
            em.close();
        }
    }

}
