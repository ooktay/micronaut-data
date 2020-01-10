package io.micronaut.data.processor.sql

import io.micronaut.data.annotation.Query
import io.micronaut.data.intercept.annotation.DataMethod
import io.micronaut.data.model.Pageable
import io.micronaut.data.processor.visitors.AbstractDataSpec
import spock.lang.Unroll

class BuildQuerySpec extends AbstractDataSpec {

    void "test join query with custom foreign key"() {
        given:
        def repository = buildRepository('test.FaceRepository', """
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;
import io.micronaut.data.tck.entities.Food;
import java.util.Optional;
import java.util.UUID;

@Repository(value = "secondary")
@JdbcRepository(dialect= Dialect.MYSQL)
interface FaceRepository extends CrudRepository<Food, UUID> {
    
    @Join("meal")
    Optional<Food> queryById(UUID uuid);
}
""")

        def query = repository.getRequiredMethod("queryById", UUID)
                .stringValue(Query).get()

        expect:
        query == 'SELECT food_.`id`,food_.`key`,food_.`carbohydrates`,food_.`portion_grams`,food_.`created_on`,food_.`updated_on`,food_.`fk_meal_id`,food_meal_.`current_blood_glucose` AS meal_current_blood_glucose,food_meal_.`created_on` AS meal_created_on,food_meal_.`updated_on` AS meal_updated_on FROM `food` food_ INNER JOIN meal food_meal_ ON food_.fk_meal_id=food_meal_.id WHERE (food_.`id` = ?)'

    }

    @Unroll
    void "test build query with datasource set"() {
        given:
        def repository = buildRepository('test.MovieRepository', """
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;

@Repository(value = "secondary")
@JdbcRepository(dialect= Dialect.MYSQL)
interface MovieRepository extends CrudRepository<Movie, Integer> {
    Optional<Movie> findByTitle(String title);
    Optional<String> findTheLongNameById(int id);
    Optional<Movie> findByTheLongName(String theLongName);
}

${entity('Movie', [title: String, theLongName: String])}
""")
        def method = repository.getRequiredMethod(methodName, arguments)

        expect:
        method.stringValue(Query).get() == query

        where:
        methodName               | arguments              | query
        'findByTitle'            |String.class            |'SELECT movie_.`id`,movie_.`title`,movie_.`the_long_name` FROM `movie` movie_ WHERE (movie_.`title` = ?)'
        'findTheLongNameById'    |int.class               |'SELECT movie_.`the_long_name` FROM `movie` movie_ WHERE (movie_.`id` = ?)'
        'findByTheLongName'      |String.class            |'SELECT movie_.`id`,movie_.`title`,movie_.`the_long_name` FROM `movie` movie_ WHERE (movie_.`the_long_name` = ?)'
    }

    void "test build DTO projection with pageable"() {
        given:
        def repository = buildRepository('test.MovieRepository', """
import io.micronaut.data.jdbc.annotation.JdbcRepository;
import io.micronaut.data.model.query.builder.sql.Dialect;

@Repository(value = "secondary")
@JdbcRepository(dialect= Dialect.MYSQL)
interface MovieRepository extends CrudRepository<Movie, Integer> {
    Page<MovieTitle> queryAll(Pageable pageable);
}

${entity('Movie', [title: String])}
${dto('MovieTitle', [title: String])}

""")
        def method = repository.getRequiredMethod("queryAll", Pageable)
        def query = method
                .stringValue(Query)
                .get()


        expect:
        method.isTrue(DataMethod, DataMethod.META_MEMBER_DTO)
        query == 'SELECT movie_.`title` AS title FROM `movie` movie_'

    }

}