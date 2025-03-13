package app.entidades;

import java.io.Serializable;

import jakarta.persistence.*;

@Entity
@Table(name = "Usuario")
public class Usuario implements Serializable {
    @Id
    String username;
    String nombre;
    String password;
    boolean administrador;
    boolean encuestador;

    @OneToOne
    Foto foto;



    public Usuario(String username, String nombre, String password, boolean administrador, boolean encuestador) {
        this.username = username;
        this.nombre = nombre;
        this.password = password;
        this.administrador = administrador;
        this.encuestador = encuestador;
        this.foto = null;
    }

    public Usuario(String username, String nombre, String password, boolean administrador, boolean encuestador, Foto foto) {
        this.username = username;
        this.nombre = nombre;
        this.password = password;
        this.administrador = administrador;
        this.encuestador = encuestador;
        this.foto = foto;
    }

    public Usuario() {

    }


    public Foto getFoto() {
        return foto;
    }

    public void setFoto(Foto foto) {
        this.foto = foto;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNombre() {

        return this.nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isAdministrador() {
        return administrador;
    }

    public void setAdministrador(boolean administrador) {
        this.administrador = administrador;
    }

    public boolean isEncuestador() {
        return encuestador;
    }

    public void setEncuestador(boolean encuestador) {
        this.encuestador = encuestador;
    }
}
