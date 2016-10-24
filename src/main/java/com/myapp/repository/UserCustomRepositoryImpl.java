package com.myapp.repository;

import com.myapp.domain.QUser;
import com.myapp.domain.User;
import com.myapp.dto.UserDTO;
import com.myapp.dto.UserStats;
import com.myapp.repository.helper.UserStatsQueryHelper;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ConstructorExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
class UserCustomRepositoryImpl implements UserCustomRepository {

    private final JPAQueryFactory queryFactory;
    private final UserRepository userRepository;

    private final QUser qUser = QUser.user;

    @Autowired
    public UserCustomRepositoryImpl(JPAQueryFactory queryFactory,
                                    UserRepository userRepository) {
        this.queryFactory = queryFactory;
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserDTO> findOne(Long userId, User currentUser) {
        final ConstructorExpression<UserStats> userStatsExpression =
                UserStatsQueryHelper.userStatsExpression(qUser, currentUser);
        final Tuple row = queryFactory.select(qUser, userStatsExpression)
                .from(qUser)
                .where(qUser.id.eq(userId))
                .fetchOne();
        return Optional.ofNullable(row)
                .map(r -> {
                    final User user = r.get(qUser);
                    final UserStats userStats = r.get(userStatsExpression);
                    assert user != null; // Row was found. It never be null.
                    final Boolean isMyself = Optional.ofNullable(currentUser)
                            .map(user::equals)
                            .orElse(null);
                    return UserDTO.newInstance(user, userStats, isMyself);
                });
    }

    @Override
    public Page<UserDTO> findAll(PageRequest pageable) {
        final Page<User> page = userRepository.findAll(pageable);
        final List<UserDTO> mappedList = page
                .getContent()
                .stream()
                .map(UserDTO::newInstance)
                .collect(Collectors.toList());
        return new PageImpl<>(mappedList, pageable, page.getTotalElements());
    }

}