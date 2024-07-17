package org.example.fourtreesproject.groupbuy.repository;

import org.example.fourtreesproject.groupbuy.model.entity.GroupBuy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupBuyRepository extends JpaRepository<GroupBuy,Long> {
}
