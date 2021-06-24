package aws.sso.role.names.mapper.domain;

import java.util.Objects;

public class PermissionSetName {
    private final String name;

    private PermissionSetName(String value) {
        this.name = Objects.requireNonNull(value, "name");
    }

    public static PermissionSetName create(String value) {
        try {
            return new PermissionSetName(value.trim().split("_")[1]);
        } catch (Exception e) {
            throw new IllegalArgumentException("Error splitting value on '_' into PermissionSetName.", e);
        }
    }

    //TODO använd inte toString för att exponera värdet i klassen. toString är en metod som ska användas för att skriva ut objektet
    //tex i en logg. Låt oss säga att ni nån gång vill bygga vidare ett objekt, tex genom att lägga till ett fält. toString borde då innehålla båda
    //Då toString alltid finns på alla objekt är det inte heller supertydligt att metoden i kontexten av just detta objekt har ett annat syfte. Går istället som nedan.


//    public String asString() {
//        return name;
//    }
//
//    @Override
//    public String toString() {
//        return "PermissionSetName{" +
//               "name='" + name + '\'' +
//               '}';
//    }



    @Override
    public String toString() {
        return name;
    }
}
