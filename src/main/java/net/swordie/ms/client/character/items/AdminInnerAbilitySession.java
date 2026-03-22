package net.swordie.ms.client.character.items;

import net.swordie.ms.client.character.Char;
import net.swordie.ms.client.character.potential.CharacterPotential;
import net.swordie.ms.connection.packet.field.FieldPacket;
import net.swordie.ms.constants.GameConstants;
import net.swordie.ms.enums.CharPotGrade;
import net.swordie.ms.enums.UIType;
import net.swordie.ms.loaders.SkillData;
import net.swordie.ms.loaders.StringData;
import net.swordie.ms.loaders.containerclasses.SkillStringInfo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class AdminInnerAbilitySession {
    private final List<Integer> candidates;
    private final List<Integer> selected = new ArrayList<>();
    private final int consumeItemId;

    public AdminInnerAbilitySession() {
        this(0);
    }

    public AdminInnerAbilitySession(int consumeItemId) {
        this.consumeItemId = consumeItemId;
        Map<Integer, Integer> bySkillId = new LinkedHashMap<>();
        for (int skillId = GameConstants.CHAR_POT_BASE_ID; skillId <= GameConstants.CHAR_POT_END_ID; skillId++) {
            if (SkillData.getSkillInfoById(skillId) == null) {
                continue;
            }
            bySkillId.put(skillId, skillId);
        }
        candidates = bySkillId.values().stream()
                .sorted(Comparator
                        .comparing((Integer skillId) -> getSkillName(skillId).toLowerCase(Locale.ENGLISH))
                        .thenComparingInt(Integer::intValue))
                .collect(Collectors.toList());
    }

    public boolean isValid() {
        return candidates.size() >= 3;
    }

    public boolean isComplete() {
        return selected.size() >= 3;
    }

    public boolean select(int index) {
        if (index < 0 || index >= candidates.size()) {
            return false;
        }
        selected.add(candidates.remove(index));
        return true;
    }

    public String getSelectionText() {
        StringBuilder sb = new StringBuilder();
        sb.append("#e<Admin Inner Ability Picker>#n\r\n");
        sb.append("Choose ").append(3 - selected.size()).append(" more Legendary line");
        if (3 - selected.size() != 1) {
            sb.append("s");
        }
        sb.append(".\r\n\r\nSelected:\r\n");
        if (selected.isEmpty()) {
            sb.append("None yet.\r\n");
        } else {
            for (int i = 0; i < selected.size(); i++) {
                sb.append(i + 1).append(". ").append(getSkillDisplay(selected.get(i))).append("\r\n");
            }
        }
        sb.append("\r\nAvailable lines:\r\n#b");
        for (int i = 0; i < candidates.size(); i++) {
            sb.append("#L").append(i).append("#").append(getSkillDisplay(candidates.get(i))).append("#l\r\n");
        }
        sb.append("#k");
        return sb.toString();
    }

    public String apply(Char chr) {
        byte grade = (byte) CharPotGrade.Legendary.ordinal();
        for (int i = 0; i < 3; i++) {
            int skillId = selected.get(i);
            int slv = Math.max(1, SkillData.getSkillInfoById(skillId).getMaxLevel());
            chr.getPotentialMan().addPotential(new CharacterPotential(chr, (byte) (i + 1), skillId, slv, grade));
        }
        if (consumeItemId > 0) {
            chr.consumeItem(consumeItemId, 1);
        }
        chr.write(FieldPacket.openUI(UIType.INNERABILITY));
        return getResultText();
    }

    private String getResultText() {
        StringBuilder sb = new StringBuilder();
        sb.append("#e<Admin Inner Ability Picker>#n\r\n");
        sb.append("Applied the following Legendary lines:\r\n\r\n");
        for (int i = 0; i < selected.size(); i++) {
            sb.append(i + 1).append(". ").append(getSkillDisplay(selected.get(i))).append("\r\n");
        }
        return sb.toString();
    }

    private String getSkillDisplay(int skillId) {
        SkillStringInfo ssi = StringData.getSkillStringById(skillId);
        String name = getSkillName(skillId);
        String desc = ssi != null ? cleanText(ssi.getDesc()) : "";
        if (desc.isEmpty()) {
            return "#g(Legendary)#k " + name + " (" + skillId + ")";
        }
        return "#g(Legendary)#k " + name + " (" + skillId + ") - " + desc;
    }

    private String getSkillName(int skillId) {
        SkillStringInfo ssi = StringData.getSkillStringById(skillId);
        if (ssi != null) {
            String name = cleanText(ssi.getName());
            if (!name.isEmpty()) {
                return name;
            }
        }
        return String.valueOf(skillId);
    }

    private String cleanText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("#", "").replace("\r", " ").replace("\n", " ").trim();
    }
}
