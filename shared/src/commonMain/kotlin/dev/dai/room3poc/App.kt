package dev.dai.room3poc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.dai.room3poc.db.databaseBuilder
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    MaterialTheme {
        // commonMainに書いた同一のDAOが全プラットフォームで動く。
        // プラットフォームごとに違うのはdatabaseBuilder()のactual（ドライバー）だけ
        val database = remember { databaseBuilder().build() }
        val dao = remember { database.dao() }
        val cats by dao.getCatsFlow().collectAsState(initial = emptyList())
        var name by remember { mutableStateOf("") }
        val scope = rememberCoroutineScope()

        Column(
            modifier =
                Modifier
                    .safeContentPadding()
                    .fillMaxSize()
                    .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Cat name") },
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        val newName = name.trim()
                        if (newName.isNotEmpty()) {
                            scope.launch { dao.insertCat(newName) }
                            name = ""
                        }
                    },
                    enabled = name.isNotBlank(),
                ) {
                    Text("Add")
                }
            }
            Text(
                text = "${cats.size} cats",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(vertical = 8.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(cats, key = { it.id }) { cat ->
                    Text(
                        text = "#${cat.id} ${cat.name}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
