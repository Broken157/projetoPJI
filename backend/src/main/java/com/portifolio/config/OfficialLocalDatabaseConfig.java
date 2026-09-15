package com.portifolio.config;

import com.portifolio.repository.*;
import com.portifolio.exception.UnprocessableEntityException;
import jakarta.persistence.Entity;
import jakarta.persistence.Converter;
import java.lang.reflect.Proxy;
import java.util.Set;
import org.springframework.context.annotation.*;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;

@Configuration
@Profile("banco-oficial-local")
@EnableJpaRepositories(basePackages="com.portifolio.repository", excludeFilters=@ComponentScan.Filter(
    type=FilterType.ASSIGNABLE_TYPE,classes={AreaArtisticaRepository.class,FuncaoRepository.class,
      EspecializacaoRepository.class,CategoriaAfirmativaRepository.class,PerfilArtistaAreaRepository.class,ResponsavelLegalRepository.class}))
public class OfficialLocalDatabaseConfig {
    private static final Set<String> MISSING = Set.of("AreaArtistica","Funcao","Especializacao","CategoriaAfirmativa","PerfilArtistaArea","ResponsavelLegal");
    @Bean
    PersistenceManagedTypes persistenceManagedTypes() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Entity.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Converter.class));
        String[] names = scanner.findCandidateComponents("com.portifolio.model").stream()
            .map(b -> b.getBeanClassName()).filter(n -> !MISSING.contains(n.substring(n.lastIndexOf('.')+1))).toArray(String[]::new);
        return PersistenceManagedTypes.of(names);
    }
    @SuppressWarnings("unchecked")
    private <T> T unavailable(Class<T> type) {
        return (T)Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(proxy,method,args)->{
            if(method.getDeclaringClass()==Object.class) return switch(method.getName()){
                case "toString" -> type.getSimpleName()+" indisponível no schema oficial";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy==args[0];
                default -> null;
            };
            throw new UnprocessableEntityException("BLOQUEADA POR SCHEMA DO BANCO: taxonomia ou estrutura ausente.");
        });
    }
    @Bean AreaArtisticaRepository areaArtisticaRepository(){return unavailable(AreaArtisticaRepository.class);}
    @Bean FuncaoRepository funcaoRepository(){return unavailable(FuncaoRepository.class);}
    @Bean EspecializacaoRepository especializacaoRepository(){return unavailable(EspecializacaoRepository.class);}
    @Bean CategoriaAfirmativaRepository categoriaAfirmativaRepository(){return unavailable(CategoriaAfirmativaRepository.class);}
    @Bean PerfilArtistaAreaRepository perfilArtistaAreaRepository(){return unavailable(PerfilArtistaAreaRepository.class);}
    @Bean ResponsavelLegalRepository responsavelLegalRepository(){return unavailable(ResponsavelLegalRepository.class);}
}
