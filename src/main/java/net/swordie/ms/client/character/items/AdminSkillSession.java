package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.Skill;
import net.swordie.ms.client.character.skills.info.SkillInfo;
import net.swordie.ms.client.character.skills.info.SkillUseInfo;
import net.swordie.ms.client.jobs.Job;
import net.swordie.ms.client.jobs.JobManager;
import net.swordie.ms.connection.packet.Effect;
import net.swordie.ms.connection.packet.UserPacket;
import net.swordie.ms.connection.packet.UserRemote;
import net.swordie.ms.constants.SkillConstants;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.SkillStringInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AdminSkillSession {
    private static final int PAGE_SIZE = 40;
    private static final int PREV_PAGE = 999998;
    private static final int NEXT_PAGE = 999999;

    private final List<Integer> skillIds = new ArrayList<>();
    private int page;
    private int selectedSkillId;

    public AdminSkillSession() {
        SkillData.getSkillInfos().forEach((skillId, skillInfo) -> {
            if (!SkillConstants.isInvisible(skillInfo)) {
                skillIds.add(skillId);
            }
        });
        skillIds.sort(Comparator.comparing(this::getSkillName, String.CASE_INSENSITIVE_ORDER)
                .thenComparingInt(Integer::intValue));
    }

    public String getSelectionText() {
        StringBuilder sb = new StringBuilder();
        sb.append("#e<Admin Skill Picker>#n\r\n");
        sb.append("Choose a visible skill to use or bind.\r\n");
        sb.append("Page ").append(page + 1).append(" / ").append(getPageCount()).append("\r\n\r\n#b");
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, skillIds.size());
        for (int i = start; i < end; i++) {
            int skillId = skillIds.get(i);
            sb.append("#L").append(i - start).append("##s").append(skillId).append("##q")
                    .append(skillId).append("# (").append(skillId).append(")#l\r\n");
        }
        if (page > 0) {
            sb.append("#L").append(PREV_PAGE).append("#< Previous Page>#l\r\n");
        }
        if (end < skillIds.size()) {
            sb.append("#L").append(NEXT_PAGE).append("#< Next Page>#l\r\n");
        }
        sb.append("#k");
        return sb.toString();
    }

    public boolean select(int selection) {
        if (selection == PREV_PAGE && page > 0) {
            page--;
            return false;
        }
        if (selection == NEXT_PAGE && (page + 1) * PAGE_SIZE < skillIds.size()) {
            page++;
            return false;
        }
        int start = page * PAGE_SIZE;
        int index = start + selection;
        if (selection < 0 || selection >= PAGE_SIZE || index >= skillIds.size()) {
            return false;
        }
        selectedSkillId = skillIds.get(index);
        return true;
    }

    public String getActionText() {
        return "#e<" + getSkillName(selectedSkillId) + ">#n\r\n\r\n#L0#Use now#l\r\n#L1#Bind to a key#l\r\n#L2#Back to list#l";
    }

    public boolean hasSelectedSkill() {
        return selectedSkillId != 0;
    }

    public void clearSelection() {
        selectedSkillId = 0;
    }

    public String useSelectedSkill(Char chr) {
        ensureSkill(chr, selectedSkillId);
        int slv = chr.getSkillLevel(selectedSkillId);
        if (!SkillConstants.isNoEncodeSkillEffect(selectedSkillId)) {
            Effect effect = Effect.skillUse(selectedSkillId, chr.getLevel(), slv, 0);
            chr.write(UserPacket.effect(effect));
            chr.getField().broadcastPacket(UserRemote.effect(chr.getId(), effect), chr);
        }
        Job jobHandler = JobManager.getJobById(SkillConstants.getSkillRootFromSkill(selectedSkillId), chr);
        if (jobHandler == null) {
            jobHandler = chr.getJobHandler();
        }
        jobHandler.handleSkill(chr, chr.getTemporaryStatManager(), selectedSkillId, slv, null, new SkillUseInfo());
        return "Used #s" + selectedSkillId + "##q" + selectedSkillId + "#.";
    }

    public String bindSelectedSkill(Char chr, int key) {
        ensureSkill(chr, selectedSkillId);
        chr.getScriptManager().setFuncKeyByScript(true, selectedSkillId, key);
        return "Bound #s" + selectedSkillId + "##q" + selectedSkillId + "# to key slot " + key + ".";
    }

    public int getSelectedSkillId() {
        return selectedSkillId;
    }

    private void ensureSkill(Char chr, int skillId) {
        if (chr.hasSkill(skillId)) {
            return;
        }
        Skill skill = SkillData.getSkillDeepCopyById(skillId);
        SkillInfo skillInfo = SkillData.getSkillInfoById(skillId);
        if (skill == null || skillInfo == null) {
            return;
        }
        int level = Math.max(Math.max(skill.getMaxLevel(), skillInfo.getMasterLevel()), Math.max(skillInfo.getFixLevel(), 1));
        skill.setCurrentLevel(level);
        skill.setMasterLevel(level);
        chr.addSkillAndSendPacket(skill);
    }

    private int getPageCount() {
        return Math.max(1, (skillIds.size() + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    private String getSkillName(int skillId) {
        SkillStringInfo ssi = StringData.getSkillStringById(skillId);
        return ssi != null && ssi.getName() != null ? ssi.getName() : String.valueOf(skillId);
    }
}
