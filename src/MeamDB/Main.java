package MeamDB;


import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        /*

        gotta somehow figure out how to communicate with the dbms

        but before that, I, Michael, will make all the UI shit if
        I can
         */




        //the user should first be prompted to log in or create
        // an account

        System.out.println("Input 'login' to log in with your credentials or 'create new account' to create a new account");



        /*
        a loop to constantly get input from users
        while true
         */

        Scanner scan = new Scanner(System.in);
        String input = "";
        //this loop runs forever to get input from the user
        while( true ){
            System.out.println("type 'help' to get a list of commands.");

            input = scan.nextLine();

            //TODO we probably want to have a function for each command
            if( input.equals("login") ){
                //in here, we need to handle logging in
            }else if( input.equals("create new account")){
                //in here, we need to handle creating a new account
                //and getting all that data.
            }else if ( input.equals("help") ){
                System.out.println("'login' to login to an existing account. You must log in before you execute other commands");
                System.out.println("'create new account' to create a new account.");
                System.out.println("'create new collection' to create a new collection of songs.");
                System.out.println("'view collections' to view all your created collections");
                System.out.println("'search song' to search for songs.");
                System.out.println("'rename collection' to rename a collection");
                System.out.println("'modify collection' to add or remove songs or albums from a collection");
                System.out.println("'delete collection' to delete a collection.");
                System.out.println("'play song' to play a song.");
                System.out.println("'play collection' to play a collection.");
                System.out.println("'follow friend' to follow a friend."); //FIXME it isn't specified, but we probably also want to be able to list the follows.
                System.out.println("'unfollow friend' to unfollow a friend.");

            }






        }


    }
}
