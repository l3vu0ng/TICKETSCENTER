package vn.ticketscenter.security;

import org.junit.jupiter.api.Test;
import vn.ticketscenter.support.VuongSqlTestSupport;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabasePrincipalsIT {
    @Test
    void mapsFourRuntimeUsersToRealLoginsAndOnlyTheirRuntimeRole() throws Exception {
        List<String> command = VuongSqlTestSupport.connectionArguments();
        command.addAll(List.of("-Q", """
                SET NOCOUNT ON;
                DECLARE @expected TABLE(userName sysname, roleName sysname);
                INSERT @expected VALUES('tc_buyer_user','tc_buyer'),('tc_manager_user','tc_manager'),
                    ('tc_checkin_user','tc_checkin'),('tc_admin_user','tc_platform_admin');
                IF EXISTS(SELECT 1 FROM @expected e LEFT JOIN sys.database_principals u ON u.name=e.userName
                    LEFT JOIN sys.server_principals l ON l.sid=u.sid WHERE u.principal_id IS NULL OR l.principal_id IS NULL)
                    THROW 51500,'Four login-backed users required',1;
                IF EXISTS(SELECT 1 FROM @expected e WHERE NOT EXISTS(SELECT 1 FROM sys.database_role_members m
                    JOIN sys.database_principals r ON r.principal_id=m.role_principal_id
                    JOIN sys.database_principals u ON u.principal_id=m.member_principal_id
                    WHERE u.name=e.userName AND r.name=e.roleName))
                    THROW 51501,'Expected runtime role membership',1;
                IF EXISTS(SELECT 1 FROM @expected e JOIN sys.database_principals u ON u.name=e.userName
                    JOIN sys.database_role_members m ON m.member_principal_id=u.principal_id
                    JOIN sys.database_principals r ON r.principal_id=m.role_principal_id WHERE r.name<>e.roleName)
                    THROW 51502,'Runtime user has extra role membership',1;
                IF EXISTS(SELECT 1 FROM @expected e JOIN sys.database_principals u ON u.name=e.userName
                    JOIN sys.server_principals l ON l.sid=u.sid
                    WHERE IS_SRVROLEMEMBER('sysadmin',l.name)=1)
                    THROW 51503,'Runtime login is sysadmin',1;
                PRINT 'VUONG-03 PRINCIPALS PASS';
                """));
        assertTrue(VuongSqlTestSupport.run(command).contains("VUONG-03 PRINCIPALS PASS"));
    }
}
