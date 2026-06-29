package ru.marketplace.finance.account.infrastructure;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.marketplace.finance.account.domain.User;

public interface UserRepository extends JpaRepository<User, Long> {

	List<User> findAllByOrderByCreatedAtDesc();

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);
}
