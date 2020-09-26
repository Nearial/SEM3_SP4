package Exceptions;

/**
 *
 * @author Nikolaj Larsen
 */
public class PersonNotFoundException extends Exception{
    public PersonNotFoundException(String message){
        super(message);
    }
}
