package app.servicios;

import app.entidades.Estudiante;
import app.entidades.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import java.util.List;

public class EstudianteServices extends GestionDb<Estudiante>{
    public EstudianteServices(){super(Estudiante.class);}
    private static EstudianteServices instancia;


    public static EstudianteServices getInstance(){
        if(instancia==null){
            instancia = new EstudianteServices();
        }
        return instancia;
    }

    public List<Estudiante> findAllByNivel(String nivelEscolar){
        EntityManager em = getEntityManager();
        Query query = em.createQuery("select e from Estudiante e where e.nivelEscolar like :nivelEscolar");
        query.setParameter("nivelEscolar", nivelEscolar + "%");
        return query.getResultList();
    }

}
