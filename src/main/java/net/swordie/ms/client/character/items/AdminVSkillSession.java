package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.skills.vmatrix.MatrixRecord;
import net.swordie.ms.connection.packet.WvsContext;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.SkillStringInfo;
import net.swordie.ms.loaders.containerclasses.VCoreInfo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminVSkillSession {
    private final List<VCoreInfo> availableInfos;
    private final List<VCoreInfo> selectedInfos = new ArrayList<>();
    private final int consumeItemId;

    public AdminVSkillSession(List<VCoreInfo> boostInfos) {
        this(boostInfos, 0);
    }

    public AdminVSkillSession(List<VCoreInfo> boostInfos, int consumeItemId) {
        this.consumeItemId = consumeItemId;
        Map<Integer, VCoreInfo> bySkillId = new LinkedHashMap<>();
        for (VCoreInfo info : boostInfos) {
            bySkillId.putIfAbsent(info.getSkillID(), info);
        }
        availableInfos = bySkillId.values().stream()
                .sorted(Comparator
                        .comparing((VCoreInfo info) -> getSkillName(info.getSkillID()).toLowerCase(Locale.ENGLISH))
                        .thenComparingInt(VCoreInfo::getSkillID))
                .collect(Collectors.toList());
    }

    public boolean isValid() {
        return availableInfos.size() >= 3;
    }

    public boolean isComplete() {
        return selectedInfos.size() >= 3;
    }

    public boolean select(int index) {
        if (index < 0 || index >= availableInfos.size()) {
            return false;
        }
        selectedInfos.add(availableInfos.remove(index));
        return true;
    }

    public String getSelectionText() {
        StringBuilder sb = new StringBuilder();
        sb.append("#e<Admin Boost Node Picker>#n\r\n");
        sb.append("Choose ").append(3 - selectedInfos.size()).append(" more skill");
        if (3 - selectedInfos.size() != 1) {
            sb.append("s");
        }
        sb.append(" to create a level 25 trio.\r\n\r\n");
        sb.append("Selected:\r\n");
        if (selectedInfos.isEmpty()) {
            sb.append("None yet.\r\n");
        } else {
            for (int i = 0; i < selectedInfos.size(); i++) {
                int skillId = selectedInfos.get(i).getSkillID();
                sb.append(i + 1).append(". #s").append(skillId).append("##q").append(skillId)
                        .append("# (").append(skillId).append(")\r\n");
            }
        }
        sb.append("\r\nAvailable boost skills:\r\n#b");
        for (int i = 0; i < availableInfos.size(); i++) {
            int skillId = availableInfos.get(i).getSkillID();
            sb.append("#L").append(i).append("##s").append(skillId).append("##q").append(skillId)
                    .append("# (").append(skillId).append(")#l\r\n");
        }
        sb.append("#k");
        return sb.toString();
    }

    public String apply(Char chr) {
        if (!isComplete()) {
            return "Selection was not completed.";
        }
        VCoreInfo mainInfo = selectedInfos.get(0);
        int[] selectedSkillIds = selectedInfos.stream().mapToInt(VCoreInfo::getSkillID).toArray();
        if (hasMatchingNode(chr, mainInfo.getIconID(), selectedSkillIds)) {
            chr.write(WvsContext.matrixUpdate(chr, false, 0, 0));
            chr.write(WvsContext.nodeOpenVmatrix(true));
            return "That trio already exists in your V Matrix.";
        }

        MatrixRecord mr = new MatrixRecord(chr);
        mr.setIconID(mainInfo.getIconID());
        mr.setMaxLevel(mainInfo.getMaxLevel());
        mr.setSlv(mr.getMaxLevel());
        mr.setSkillID1(selectedSkillIds[0]);
        mr.setSkillID2(selectedSkillIds[1]);
        mr.setSkillID3(selectedSkillIds[2]);
        chr.addMatrixRecord(mr);
        if (consumeItemId > 0) {
            chr.consumeItem(consumeItemId, 1);
        }
        chr.write(WvsContext.matrixUpdate(chr, false, 0, 0));
        chr.write(WvsContext.nodeOpenVmatrix(true));
        return "Created level 25 trio: " + formatSelectedSkills();
    }

    private boolean hasMatchingNode(Char chr, int iconId, int[] selectedSkillIds) {
        int[] expected = Arrays.stream(selectedSkillIds).filter(skillId -> skillId != 0).sorted().toArray();
        return chr.getSortedMatrixRecords().stream()
                .filter(mr -> mr.getIconID() == iconId)
                .anyMatch(mr -> Arrays.equals(
                        Arrays.stream(mr.getSkills()).filter(skillId -> skillId != 0).sorted().toArray(),
                        expected
                ));
    }

    private String formatSelectedSkills() {
        return selectedInfos.stream()
                .map(info -> "#s" + info.getSkillID() + "##q" + info.getSkillID() + "#")
                .collect(Collectors.joining(", "));
    }

    private String getSkillName(int skillId) {
        SkillStringInfo ssi = StringData.getSkillStringById(skillId);
        return ssi != null && ssi.getName() != null ? ssi.getName() : String.valueOf(skillId);
    }
}
