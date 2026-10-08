package vn.ticketscenter.repository.identity;

import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import vn.ticketscenter.model.identity.User;

/**
 * Repository for User persistence. Receives EntityManager from caller's transaction. Does NOT open
 * its own transaction or EntityManagerFactory.
 */
public class UserRepository {

  private static final UserRepository INSTANCE = new UserRepository();

  private UserRepository() {}

  public static UserRepository getInstance() {
    return INSTANCE;
  }

  public Optional<User> findById(EntityManager em, UUID id) {
    return Optional.ofNullable(em.find(User.class, id));
  }

  public Optional<User> findByNormalizedEmail(EntityManager em, String normalizedEmail) {
    return em
        .createQuery("SELECT u FROM User u WHERE u.normalizedEmail = :email", User.class)
        .setParameter("email", normalizedEmail)
        .setMaxResults(1)
        .getResultList()
        .stream()
        .findFirst();
  }

  public Optional<User> findByNormalizedUserName(EntityManager em, String normalizedUserName) {
    return em
        .createQuery("SELECT u FROM User u WHERE u.normalizedUserName = :uname", User.class)
        .setParameter("uname", normalizedUserName)
        .setMaxResults(1)
        .getResultList()
        .stream()
        .findFirst();
  }

  public void persist(EntityManager em, User user) {
    em.persist(user);
  }
}
