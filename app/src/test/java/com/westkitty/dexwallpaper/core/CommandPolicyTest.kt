package com.westkitty.dexwallpaper.core
import org.junit.Assert.*
import org.junit.Test
class CommandPolicyTest {
    @Test fun defaultExternalTargetIsHomeOnly() { assertEquals(Destination.HOME,(CommandPolicy.parse("NEXT",null,null,null) as Outcome.Ok).value.destination) }
    @Test fun unknownTargetsNeverBecomeBoth() { assertTrue(CommandPolicy.parse("NEXT","all",null,null) is Outcome.Error) }
    @Test fun arbitraryCodeAndPathsRejected() { assertTrue(CommandPolicy.parse("EXEC","HOME","/private/file",null) is Outcome.Error); assertTrue(CommandPolicy.parse("APPLY","HOME","../../secret",null) is Outcome.Error) }
    @Test fun existingLibraryIdentityIsRequired() { assertTrue(CommandPolicy.parse("APPLY","HOME",null,null) is Outcome.Error); assertTrue(CommandPolicy.parse("SWITCH_COLLECTION","HOME",null,"default") is Outcome.Ok) }
    @Test fun bothOnlyWhenExplicitlyRequested() { assertEquals(Destination.BOTH,(CommandPolicy.parse("APPLY","BOTH","asset",null) as Outcome.Ok).value.destination) }
}
