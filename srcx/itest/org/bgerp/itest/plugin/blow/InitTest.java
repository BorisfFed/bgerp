package org.bgerp.itest.plugin.blow;

import org.testng.annotations.Test;

@Test(groups = "blowInit", dependsOnGroups = "configInit")
public class InitTest {
    @Test
    public void initConfig() throws Exception {
        
    }
}