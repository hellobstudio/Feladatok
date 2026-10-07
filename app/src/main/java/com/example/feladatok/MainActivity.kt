package com.example.feladatok

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feladatok.data.*
import com.example.feladatok.ui.TaskViewModel
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    private val viewModel: TaskViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                TaskAppScreen(viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAppScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val sortMode by viewModel.sortMode.collectAsState()
    val filterMode by viewModel.filterMode.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Feladatok") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Új feladat")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Rendezés:", style = MaterialTheme.typography.labelLarge)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                FilterChip(
                    selected = sortMode == SortMode.BY_DATE,
                    onClick = { viewModel.sortMode.value = SortMode.BY_DATE },
                    label = { Text("Dátum") }
                )
                FilterChip(
                    selected = sortMode == SortMode.BY_PRIORITY_THEN_DATE,
                    onClick = { viewModel.sortMode.value = SortMode.BY_PRIORITY_THEN_DATE },
                    label = { Text("Sürgősség >> Dátum") }
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                FilterChip(
                    selected = filterMode == FilterMode.ALL,
                    onClick = { viewModel.filterMode.value = FilterMode.ALL },
                    label = { Text("Összes") }
                )
                FilterChip(
                    selected = filterMode == FilterMode.PENDING,
                    onClick = { viewModel.filterMode.value = FilterMode.PENDING },
                    label = { Text("Teendő") }
                )
                FilterChip(
                    selected = filterMode == FilterMode.COMPLETED,
                    onClick = { viewModel.filterMode.value = FilterMode.COMPLETED },
                    label = { Text("Kész") }
                )
            }

            HorizontalDivider()

            if (tasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nincs megjeleníthető feladat.")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    items(tasks, key = { it.id }) { task ->
                        TaskItem(
                            task = task,
                            onToggleComplete = { viewModel.toggleTaskCompletion(task) },
                            onDelete = { viewModel.deleteTask(task) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddTaskDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { title, deadline, priority ->
                viewModel.addTask(title, deadline, priority)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun TaskItem(task: Task, onToggleComplete: () -> Unit, onDelete: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("yyyy.MM.dd", Locale.getDefault()) }
    val isOverdue = task.deadlineMillis < System.currentTimeMillis() && !task.isCompleted

    val priorityColor = when (task.priority) {
        Priority.URGENT -> Color(0xFFE53935)
        Priority.NORMAL -> Color(0xFFFB8C00)
        Priority.LOW -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggleComplete() }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp)
            ) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(priorityColor, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = task.priority.label,
                            color = Color.White,
                            fontSize = 10.sp
                        )
                    }

                    Text(
                        text = "Határidő: ${dateFormat.format(Date(task.deadlineMillis))}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isOverdue) Color.Red else Color.Gray
                    )
                }
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Törlés",
                    tint = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, Long, Priority) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.NORMAL) }
    
    val calendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
    var selectedDeadlineMillis by remember { mutableLongStateOf(calendar.timeInMillis) }

    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDeadlineMillis)
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Új feladat rögzítése") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Feladat neve") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Fontossági sorrend:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Priority.entries.forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.label) }
                        )
                    }
                }

                Button(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val formattedDate = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
                        .format(Date(selectedDeadlineMillis))
                    Text("Határidő: $formattedDate")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onConfirm(title, selectedDeadlineMillis, priority)
                    }
                }
            ) {
                Text("Hozzáadás")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Mégse")
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDeadlineMillis = it
                    }
                    showDatePicker = false
                }) { Text("OK") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
