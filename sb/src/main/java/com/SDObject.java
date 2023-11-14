package com;

import java.util.*;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
public class SDObject {
    public static void main(String[] args) throws IOException {}
    /** Read the object from Base64 string. */
   public Object fromString( String s ) throws IOException,ClassNotFoundException {
        byte [] data = Base64.getDecoder().decode( s );
        ObjectInputStream ois = new ObjectInputStream( 
                                        new ByteArrayInputStream(data));
        Object o  = ois.readObject();
        ois.close();
        return o;
   }

    /** Write the object to a Base64 string. */
    public String toString( Serializable o ) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( baos );
        oos.writeObject( o );
        oos.close();
        return Base64.getEncoder().encodeToString(baos.toByteArray()); 
    }
}
