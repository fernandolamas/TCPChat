/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Encrypt;

/**
 *
 * @author diego
 */
public class Main {
    public static void main(String[] args) throws Exception {
 String key = "92AE31A79FEEB2A3"; //llave
 String iv = "0123456789ABCDEF"; // vector de inicialización
 String cleartext = "La Vida Vale Menos Que El Amor";
 String encryptedtext = Encrypt.StringEncrypt.encrypt(key, iv,cleartext);
 System.out.println("Texto encriptado: "+Encrypt.StringEncrypt.encrypt(key, iv,cleartext));
 System.out.println("Texto desencriptado: "+Encrypt.StringEncrypt.decrypt(key, iv,encryptedtext));
 }
    
}
