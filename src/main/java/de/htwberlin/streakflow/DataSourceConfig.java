package de.htwberlin.streakflow;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.net.URI;

@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        String springUrl = System.getenv("SPRING_DATASOURCE_URL");
        String renderUrl = System.getenv("DATABASE_URL");

        if (springUrl != null && !springUrl.isBlank()) {
            return buildDataSource(
                    normalizePostgresUrl(springUrl),
                    System.getenv("SPRING_DATASOURCE_USERNAME"),
                    System.getenv("SPRING_DATASOURCE_PASSWORD")
            );
        }

        if (renderUrl != null && !renderUrl.isBlank()) {
            return buildDataSourceFromRenderUrl(renderUrl);
        }

        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.h2.Driver");
        dataSource.setUrl("jdbc:h2:mem:streakflow;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        dataSource.setUsername("sa");
        dataSource.setPassword("");
        return dataSource;
    }

    private DataSource buildDataSource(String url, String username, String password) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(url);
        dataSource.setUsername(username == null ? "" : username);
        dataSource.setPassword(password == null ? "" : password);
        return dataSource;
    }

    private DataSource buildDataSourceFromRenderUrl(String databaseUrl) {
        URI uri = URI.create(databaseUrl);
        String[] userInfo = uri.getUserInfo() == null ? new String[]{"", ""} : uri.getUserInfo().split(":", 2);
        String username = userInfo.length > 0 ? userInfo[0] : "";
        String password = userInfo.length > 1 ? userInfo[1] : "";
        String jdbcUrl = "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();

        return buildDataSource(jdbcUrl, username, password);
    }

    private String normalizePostgresUrl(String url) {
        if (url.startsWith("postgres://") || url.startsWith("postgresql://")) {
            URI uri = URI.create(url);
            return "jdbc:postgresql://" + uri.getHost() + ":" + uri.getPort() + uri.getPath();
        }

        return url;
    }
}
