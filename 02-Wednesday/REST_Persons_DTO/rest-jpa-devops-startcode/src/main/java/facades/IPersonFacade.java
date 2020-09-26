package facades;

import DTO.PersonDTO;
import DTO.PersonsDTO;
import Exceptions.MissingInputException;
import Exceptions.PersonNotFoundException;

/**
 *
 * @author Nikolaj Larsen
 */
public interface IPersonFacade {
  public PersonDTO addPerson(String fName, String lName, String phone, String street, int zip, String city) throws MissingInputException;  
  public PersonDTO deletePerson(int id) throws PersonNotFoundException;  
  public PersonDTO getPerson(int id) throws PersonNotFoundException;  
  public PersonsDTO getAllPersons();  
  public PersonDTO editPerson(PersonDTO p) throws PersonNotFoundException, MissingInputException;  

}
