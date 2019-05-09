package io.micronaut.data.processor.visitors

import io.micronaut.annotation.processing.TypeElementVisitorProcessor
import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.annotation.processing.test.JavaParser
import io.micronaut.data.annotation.Query
import io.micronaut.data.intercept.FindAllByInterceptor
import io.micronaut.data.intercept.FindOneInterceptor
import io.micronaut.data.intercept.annotation.PredatorMethod
import io.micronaut.data.model.query.encoder.entities.Person
import io.micronaut.inject.BeanDefinition
import io.micronaut.inject.beans.visitor.IntrospectedTypeElementVisitor
import io.micronaut.inject.visitor.TypeElementVisitor
import io.micronaut.inject.writer.BeanDefinitionVisitor
import spock.lang.Specification

import javax.annotation.processing.SupportedAnnotationTypes

class JpaOrderBySpec extends AbstractTypeElementSpec {

    void "test order by method definitions"() {
        given:
        BeanDefinition beanDefinition = buildBeanDefinition('test.MyInterface' + BeanDefinitionVisitor.PROXY_SUFFIX, """
package test;

import io.micronaut.data.model.query.encoder.entities.Person;
import io.micronaut.data.repository.CrudRepository;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.annotation.Query;
import java.util.List;

@Repository
interface MyInterface extends io.micronaut.data.repository.Repository<Person, Long> {

    List<Person> queryByNameOrderByName(String n);
    
    List<Person> listOrderByName();
    
    List<String> listNameOrderByName();
    
}
""")

        when: "the query method is retrieved"
        def findOne = beanDefinition.getRequiredMethod("queryByNameOrderByName", String.class)
        def list = beanDefinition.getRequiredMethod("listOrderByName")
        def listName = beanDefinition.getRequiredMethod("listNameOrderByName")

        then: "It was correctly compiled"
        findOne.synthesize(Query).value() == "SELECT person FROM $Person.name AS person WHERE (person.name = :p1) ORDER BY person.name ASC"
        list.synthesize(Query).value() == "SELECT person FROM $Person.name AS person ORDER BY person.name ASC"
        list.synthesize(PredatorMethod).resultType() == Person
        listName.synthesize(Query).value() == "SELECT person.name FROM $Person.name AS person ORDER BY person.name ASC"
        listName.synthesize(PredatorMethod).resultType() == String
    }

    @Override
    protected JavaParser newJavaParser() {
        return new JavaParser() {
            @Override
            protected TypeElementVisitorProcessor getTypeElementVisitorProcessor() {
                return new MyTypeElementVisitorProcessor()
            }
        }
    }

    @SupportedAnnotationTypes("*")
    static class MyTypeElementVisitorProcessor extends TypeElementVisitorProcessor {
        @Override
        protected Collection<TypeElementVisitor> findTypeElementVisitors() {
            return [new IntrospectedTypeElementVisitor(), new RepositoryTypeElementVisitor()]
        }
    }
}
