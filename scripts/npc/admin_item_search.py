from net.swordie.ms.loaders import StringData

MAX_RESULTS = 100


def _safe_trim(text):
    if text is None:
        return ""
    return str(text).strip()


def _to_lower_list(values):
    out = []
    if values is None:
        return out
    for value in values:
        trimmed = _safe_trim(value)
        if trimmed != "":
            out.append(trimmed.lower())
    return out


def _matches_filters(item_name, include_terms, exclude_terms):
    lower_name = item_name.lower()
    for include_term in include_terms:
        if include_term not in lower_name:
            return False
    for exclude_term in exclude_terms:
        if exclude_term in lower_name:
            return False
    return True


query = _safe_trim(initial_query if "initial_query" in globals() else "")
exclude_terms = _to_lower_list(
    exclude_queries if "exclude_queries" in globals() else []
)

if query == "" and len(exclude_terms) == 0:
    query = _safe_trim(sm.sendAskText("Search items by name:", "", 1, 40))

if query == "":
    sm.sendSayOkay("Please enter at least 1 include term.")
else:
    include_terms = _to_lower_list(query.split(" "))
    result_map = StringData.getItemStringByName(query, False)
    if result_map is None or len(result_map) == 0:
        result_map = StringData.getItemStringByName(include_terms[0], False)

    if result_map is None or len(result_map) == 0:
        sm.sendSayOkay("No items were found for '#b" + query + "#k'.")
    else:
        results = []
        for item_id in result_map.keySet():
            name = result_map.get(item_id)
            if name is None:
                continue
            item_name = str(name)
            if not _matches_filters(item_name, include_terms, exclude_terms):
                continue
            try:
                iid = int(item_id)
            except Exception:
                continue
            results.append((iid, item_name))

        if len(results) == 0:
            display_query = query
            for ex in exclude_terms:
                display_query += " !" + ex
            sm.sendSayOkay("No valid items were found for '#b" + display_query + "#k'.")
        else:
            results = sorted(results, key=lambda it: (it[1].lower(), it[0]))
            if len(results) > MAX_RESULTS:
                results = results[:MAX_RESULTS]

            display_query = query
            for ex in exclude_terms:
                display_query += " !" + ex
            msg = (
                "Search results for '#b"
                + display_query
                + "#k' (showing up to "
                + str(MAX_RESULTS)
                + "):\r\n\r\n"
            )
            for i in range(len(results)):
                item_id = results[i][0]
                msg += (
                    "#L"
                    + str(i)
                    + "##v"
                    + str(item_id)
                    + "# #z"
                    + str(item_id)
                    + "##l\r\n"
                )

            sel = sm.sendNext(msg)
            if sel < 0 or sel >= len(results):
                sm.sendSayOkay("Invalid selection.")
            else:
                chosen_item_id = results[sel][0]
                quantity = sm.sendAskNumber(
                    "Enter quantity for #v"
                    + str(chosen_item_id)
                    + "# #z"
                    + str(chosen_item_id)
                    + "#:",
                    1,
                    1,
                    30000,
                )
                if quantity <= 0:
                    sm.sendSayOkay("Invalid quantity.")
                elif not sm.canHold(chosen_item_id):
                    sm.sendSayOkay("Please make room in your inventory first.")
                else:
                    sm.giveItem(chosen_item_id, quantity)
                    sm.chat("Debug: selected itemID = " + str(chosen_item_id))
                    sm.sendSayOkay(
                        "You received "
                        + str(quantity)
                        + "x #v"
                        + str(chosen_item_id)
                        + "# #z"
                        + str(chosen_item_id)
                        + "#."
                    )
