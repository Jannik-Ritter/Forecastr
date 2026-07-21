package de.eva.forecastr.repository;

import de.eva.forecastr.core.models.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
  @Query("select u from User u where u.deletedAt is null order by lower(u.username)")
  List<User> findByDeletedAtIsNullOrderByUsernameAsc();

  Page<User> findByDeletedAtIsNull(Pageable pageable);

  long countByDeletedAtIsNull();

  Optional<User> findByUsernameIgnoreCase(String username);

  boolean existsByUsernameIgnoreCase(String username);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select u from User u where u.id = :id")
  Optional<User> findLocked(@Param("id") Long id);
}
