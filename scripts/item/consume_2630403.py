from java.util import ArrayList
from net.swordie.ms.client.character.items import AdminVSkillSession
from net.swordie.ms.constants import QuestConstants
from net.swordie.ms.loaders import VCoreData

if not chr.getQuestManager().hasQuestCompleted(QuestConstants.FIFTH_JOB_QUEST):
    sm.sendSayOkay("You need to be 5th job to use this item.")
else:
    infos = VCoreData.getPossibilitiesByJob(chr.getJob())
    if infos is None:
        sm.sendSayOkay("No V Matrix data exists for your current job.")
    else:
        boostInfos = ArrayList()
        for info in infos:
            if info.isEnforce():
                boostInfos.add(info)
        session = AdminVSkillSession(boostInfos, parentID)
        if not session.isValid():
            sm.sendSayOkay("Your current job does not have enough boost node skills to build a trio.")
        else:
            while not session.isComplete():
                sel = sm.sendNext(session.getSelectionText())
                if not session.select(sel):
                    sm.sendSayOkay("Invalid selection.")
                    break

            if session.isComplete():
                sm.sendSayOkay(session.apply(chr))
