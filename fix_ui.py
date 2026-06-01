import re

with open('app/src/main/java/com/poketft/overlay/CalculatorOverlay.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# 1. PokemonAutocompleteField
old_auto = """        OutlinedTextField(
            value = query,
            onValueChange = { text ->
                query = text
                showSuggestions = text.isNotBlank()
            },
            placeholder = {
                Text(
                    if (dbCount > 0) "이름 검색 (${dbCount}종)" else "DB 로드 실패",
                    fontSize = 9.sp,
                    color = PokeTextSec
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .onFocusChanged { focus ->
                    onRequestFocus(focus.isFocused)
                    if (focus.isFocused && query.isNotBlank()) showSuggestions = true
                },
            singleLine = true,
            enabled = dbCount > 0,
            textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, color = PokeTextPri),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PokeTextPri,
                unfocusedTextColor = PokeTextPri,
                disabledTextColor = PokeTextPri,
                focusedBorderColor = PokeAccent,
                unfocusedBorderColor = PokeBorder,
                cursorColor = PokeAccent
            )
        )"""

new_auto = """        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = { text ->
                query = text
                showSuggestions = text.isNotBlank()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .onFocusChanged { focus ->
                    onRequestFocus(focus.isFocused)
                    if (focus.isFocused && query.isNotBlank()) showSuggestions = true
                }
                .border(1.dp, PokeBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 8.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 10.sp, color = PokeTextPri),
            singleLine = true,
            enabled = dbCount > 0,
            cursorBrush = androidx.compose.ui.graphics.SolidColor(PokeAccent),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.fillMaxSize()) {
                    if (query.isEmpty()) {
                        Text(
                            if (dbCount > 0) "이름 검색 (${dbCount}종)" else "DB 로드 실패",
                            fontSize = 9.sp,
                            color = PokeTextSec
                        )
                    }
                    innerTextField()
                }
            }
        )"""
content = content.replace(old_auto, new_auto)

# 2. EnvMiniDropdown
old_env = """            OutlinedTextField(
                value = display,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(30.dp),
                textStyle = LocalTextStyle.current.copy(fontSize = 8.sp, color = PokeTextPri),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PokeTextPri,
                    unfocusedTextColor = PokeTextPri,
                    disabledTextColor = PokeTextPri,
                    focusedBorderColor = PokeAccent,
                    unfocusedBorderColor = PokeBorder,
                    disabledBorderColor = PokeBorder
                ),
                singleLine = true
            )"""
new_env = """            Box(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .height(30.dp)
                    .border(1.dp, if (expanded) PokeAccent else PokeBorder, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(display, fontSize = 8.sp, color = PokeTextPri)
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }"""
content = content.replace(old_env, new_env)

# 3. CompactDropdown
old_compact = """        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(32.dp),
            textStyle = LocalTextStyle.current.copy(fontSize = 9.sp, color = PokeTextPri),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = PokeTextPri,
                unfocusedTextColor = PokeTextPri,
                disabledTextColor = PokeTextPri,
                focusedBorderColor = PokeAccent,
                unfocusedBorderColor = PokeBorder,
                disabledBorderColor = PokeBorder
            ),
            singleLine = true
        )"""
new_compact = """        Box(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
                .height(32.dp)
                .border(1.dp, if (expanded) PokeAccent else PokeBorder, RoundedCornerShape(4.dp))
                .clickable(enabled = enabled) { expanded = !expanded }
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = value,
                fontSize = 9.sp,
                color = if (enabled) PokeTextPri else PokeTextSec,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 24.dp)
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }
        }"""
content = content.replace(old_compact, new_compact)

with open('app/src/main/java/com/poketft/overlay/CalculatorOverlay.kt', 'w', encoding='utf-8') as f:
    f.write(content)
print("DONE PATCHING")
