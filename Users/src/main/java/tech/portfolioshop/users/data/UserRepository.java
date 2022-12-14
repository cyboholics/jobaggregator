package tech.portfolioshop.users.data;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends CrudRepository<UserEntity, Long> {
    @Query("SELECT u FROM UserEntity u WHERE u.email = ?1 AND u.status = true")
    UserEntity findByEmail(String email);
    @Query("SELECT u FROM UserEntity u WHERE u.userId = ?1 AND u.status = true")
    UserEntity findByUserId(String userId);
}
