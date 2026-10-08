package vn.ticketscenter.service.identity;

import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class CsrfTokens {
  public static final String ATTRIBUTE = "csrfToken";
  private static final SecureRandom RANDOM = new SecureRandom();

  private CsrfTokens() {}

  public static String getOrCreate(HttpSession session) {
    synchronized (session) {
      Object token = session.getAttribute(ATTRIBUTE);
      if (token instanceof String text) return text;
      return rotate(session);
    }
  }

  public static String rotate(HttpSession session) {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    session.setAttribute(ATTRIBUTE, token);
    return token;
  }

  public static boolean matches(HttpSession session, String candidate) {
    if (session == null || candidate == null || candidate.length() != 43) return false;
    Object expected = session.getAttribute(ATTRIBUTE);
    return expected instanceof String token
        && MessageDigest.isEqual(
            token.getBytes(StandardCharsets.US_ASCII),
            candidate.getBytes(StandardCharsets.US_ASCII));
  }
}
