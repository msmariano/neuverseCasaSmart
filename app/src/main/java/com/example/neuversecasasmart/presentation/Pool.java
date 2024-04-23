package com.example.neuversecasasmart.presentation;


import com.google.gson.annotations.Expose;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Pool {
    @Expose(serialize = true)
    private String id;
    @Expose(serialize = true)
    private List<Dispositivo> dispositivos = new ArrayList<>();
    @Expose(serialize = true)
    private String nick;

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }


    public Dispositivo buscar(Integer id){
        for (Dispositivo dispositivo : dispositivos){
            if(dispositivo.getId().equals(id))
                return dispositivo;
        }
        return null;
    }


    public Pool(){
        UUID uniqueKey = UUID.randomUUID();
        String idGerado = uniqueKey.toString();
        id = idGerado;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Dispositivo> getDispositivos() {
        return dispositivos;
    }

    public void setDispositivos(List<Dispositivo> dispositivos) {
        this.dispositivos = dispositivos;
    }

}
