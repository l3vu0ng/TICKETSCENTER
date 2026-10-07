package vn.ticketscenter.repository.identity;

import jakarta.persistence.EntityManager;
import vn.ticketscenter.model.identity.User;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for User persistence. Receives EntityManager from caller's transaction.
 * Does NOT open its own transaction or EntityManagerFactory.
 * Owner: Khánh (KHANH-04)
 */
public class UserRepository {

    private static final UserRepository INSTANCE = new UserRepository();
    private UserRepository() {}
    public static UserRepository getInstance() { return INSTANCE; }

    public Optional<User> findById(EntityManager em, UUID id) {
        return Optional.ofNullable(em.find(User.class, id));
    }

    public Optional<User> findByNormalizedEmail(EntityManager em, String normalizedEmail) {
        return em.createQuery(
                "SELECT u FROM User u WHERE u.normalizedEmail = :email", User.class)
                .setParameter("email", normalizedEmail)
                .getResultStream()
                .findFirst();
    }

    public Optional<User> findByNormalizedUserName(EntityManager em, String normalizedUserName) {
        return em.createQuery(
                "SELECT u FROM User u WHERE u.normalizedUserName = :uname", User.class)
                .setParameter("uname", normalizedUserName)
                .getResultStream()
                .findFirst();
    }

    public void persist(EntityManager em, User user) {
        em.persist(user);
    }
}
