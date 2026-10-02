package kr.fast.Jejuro.Config;


// [공통]

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.naming.PhysicalNamingStrategy;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;

/**
 * 테이블·칼럼 이름 규칙.
 * - 테이블: 항상 소문자로 쓴다(@Table(name = "TRAVEL") → travel).
 *   DB 덤프(DB/jejuro_*.sql)의 테이블 이름이 소문자이므로, 대소문자를 구분하는 Linux MySQL
 *   (lower_case_table_names=0)과 구분하지 않는 Windows MySQL 어디서든 같은 이름으로 찾는다.
 *   직접 쓰는 SQL(JdbcTemplate·native query)도 테이블 이름은 소문자로 적는다.
 * - 칼럼: 자바 필드 travelName → DB 칼럼 travel_name 으로 바꾼다.
 */
public class LowerCaseTableNamingStrategy implements PhysicalNamingStrategy {

    @Override
    public Identifier toPhysicalCatalogName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return name;
    }

    @Override
    public Identifier toPhysicalSchemaName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return name;
    }

    @Override
    public Identifier toPhysicalTableName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        if (name == null) {
            return null;
        }
        return Identifier.toIdentifier(name.getText().toLowerCase(java.util.Locale.ROOT), name.isQuoted());
    }

    @Override
    public Identifier toPhysicalSequenceName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        return name;
    }

    @Override
    public Identifier toPhysicalColumnName(Identifier name, JdbcEnvironment jdbcEnvironment) {
        if (name == null) {
            return null;
        }
        return Identifier.toIdentifier(toSnakeCase(name.getText()), name.isQuoted());
    }

    /** travelName → travel_name, adoptedRouteId → adopted_route_id */
    private static String toSnakeCase(String text) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && text.charAt(i - 1) != '_') {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}