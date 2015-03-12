package client;

/**
 * Basic implementation of AbstractRoute.
 * @author Jaco
 * @version 09-03-2015
 */
public class BasicRoute extends AbstractRoute {
    public String toString() {
        return Integer.toString(nextHop);
    }
}
