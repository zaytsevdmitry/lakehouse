package org.lakehouse.config.mapper.keyvalue;

import org.springframework.data.jpa.repository.JpaRepository;

public abstract class KeyValueEntitySpecifierAbstract implements KeyValueEntitySpecifier{
    private final JpaRepository jpaRepository;

    protected KeyValueEntitySpecifierAbstract(JpaRepository getJpaRepository) {
        this.jpaRepository = getJpaRepository;
    }

    @Override
    public JpaRepository getJpaRepository(){
        return jpaRepository;
    }
}
