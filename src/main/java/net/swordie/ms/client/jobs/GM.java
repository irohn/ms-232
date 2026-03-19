package net.swordie.ms.client.jobs;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.character.skills.temp.TemporaryStatManager;
import net.swordie.ms.connection.InPacket;
import net.swordie.ms.constants.JobConstants;

public class GM extends Job {
    public GM(Char chr) {
        super(chr);
    }

    @Override
    public void handleSkill(Char chr, TemporaryStatManager tsm, int skillId, int slv, InPacket inPacket, SkillUseInfo skillUseInfo) {
        super.handleSkill(chr, tsm, skillId, slv, inPacket, skillUseInfo);
    }

    @Override
    public boolean isHandlerOfJob(short id) {
        return JobConstants.isGmJob(id);
    }
}
