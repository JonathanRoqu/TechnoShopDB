package com.example.technoshopdb;

import java.util.Base64;

public class utilidades {
    static String urlConsulta = "http://http://192.168.130.250:5984/productos/_design/productos/_view/productos";
    static String urlMto = "http://192.168.130.250:5984/productos";
    static String user = "admin";
    static String passwd = "josueing";
    static String credencialesCodificadas = Base64.getEncoder().encodeToString((user + ":"+ passwd).getBytes());
    static String generarIdUnico() {return java.util.UUID.randomUUID().toString();}
}
