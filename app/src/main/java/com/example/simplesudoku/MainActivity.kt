package com.example.simplesudoku // Zostaw tutaj nazwę swojego pakietu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SudokuGameScreen()
                }
            }
        }
    }
}

enum class Difficulty(val label: String, val emptyCells: Int) {
    EASY("Łatwy", 30),
    MEDIUM("Średni", 40),
    HARD("Trudny", 50),
    EXPERT("Ekspert", 58)
}

data class Cell(
    val row: Int,
    val col: Int,
    val value: Int = 0,
    val solution: Int = 0,
    val isInitial: Boolean = false,
    val notes: Set<Int> = emptySet() // Przechowuje notatki ołówkiem (cyfry 1-9)
)

@Composable
fun SudokuGameScreen() {
    var difficulty by remember { mutableStateOf(Difficulty.EASY) }
    var board by remember { mutableStateOf(generateSudoku(difficulty)) }
    var selectedCell by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var isNoteMode by remember { mutableStateOf(false) } // Stan trybu notatek (ołówka)

    val selectedValue = selectedCell?.let { (r, c) -> board[r][c].value } ?: 0

    val isSolved = remember(board) {
        board.flatten().all { it.value != 0 && it.value == it.solution }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // --- GÓRNY PANEL: Wybór poziomu trudności ---
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "SUDOKU",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Difficulty.entries.forEach { diff ->
                    Button(
                        onClick = {
                            difficulty = diff
                            board = generateSudoku(diff)
                            selectedCell = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (difficulty == diff) MaterialTheme.colorScheme.primary else Color.Gray
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(diff.label, fontSize = 12.sp, maxLines = 1)
                    }
                }
            }
        }

        // --- KOMUNIKAT O WYGRANEJ ---
        if (isSolved) {
            Text(
                text = "Gratulacje! Rozwiązano poprawnie!",
                color = Color(0xFF2E7D32),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // --- PLANSZA SUDOKU 9x9 ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .border(2.dp, Color.Black)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                for (r in 0 until 9) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        for (c in 0 until 9) {
                            val cell = board[r][c]
                            val isSelected = selectedCell == Pair(r, c)
                            val isSameNumber = selectedValue != 0 && cell.value == selectedValue
                            val isError = cell.value != 0 && cell.value != cell.solution

                            val bgColor = when {
                                isSelected -> Color(0xFF64B5F6)
                                isSameNumber -> Color(0xFFBBDEFB)
                                else -> Color.White
                            }

                            val textColor = when {
                                cell.isInitial -> Color.Black
                                isError -> Color.Red
                                else -> Color(0xFF1565C0)
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .background(bgColor)
                                    .clickable { selectedCell = Pair(r, c) }
                                    .drawBehind {
                                        val strokeRight = if ((c + 1) % 3 == 0 && c != 8) 3.dp.toPx() else 0.5.dp.toPx()
                                        val strokeBottom = if ((r + 1) % 3 == 0 && r != 8) 3.dp.toPx() else 0.5.dp.toPx()

                                        drawLine(
                                            color = Color.Black,
                                            start = Offset(size.width, 0f),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = strokeRight
                                        )
                                        drawLine(
                                            color = Color.Black,
                                            start = Offset(0f, size.height),
                                            end = Offset(size.width, size.height),
                                            strokeWidth = strokeBottom
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (cell.value != 0) {
                                    // Wyświetlanie głównej wpisanej cyfry
                                    Text(
                                        text = cell.value.toString(),
                                        fontSize = 22.sp,
                                        fontWeight = if (cell.isInitial || isSameNumber) FontWeight.Bold else FontWeight.Normal,
                                        color = textColor
                                    )
                                } else if (cell.notes.isNotEmpty()) {
                                    // Wyświetlanie siatki 3x3 dla notatek ołówkiem
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(1.dp),
                                        verticalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        for (nr in 0..2) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .weight(1f),
                                                horizontalArrangement = Arrangement.SpaceEvenly,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                for (nc in 0..2) {
                                                    val noteNum = nr * 3 + nc + 1
                                                    val hasNote = cell.notes.contains(noteNum)
                                                    val isHighlightedNote = selectedValue != 0 && selectedValue == noteNum

                                                    Text(
                                                        text = if (hasNote) noteNum.toString() else "",
                                                        fontSize = 9.sp,
                                                        lineHeight = 9.sp,
                                                        fontWeight = if (isHighlightedNote) FontWeight.ExtraBold else FontWeight.Normal,
                                                        color = if (isHighlightedNote) Color(0xFF0D47A1) else Color.DarkGray,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- DOLNY PANEL: Klawiatura numeryczna i przyciski akcji ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Cyfry 1-9
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                for (num in 1..9) {
                    Button(
                        onClick = {
                            selectedCell?.let { (r, c) ->
                                val targetCell = board[r][c]
                                if (!targetCell.isInitial && !isSolved) {
                                    if (isNoteMode) {
                                        // Tryb notatek: dodaj lub usuń notatkę (tylko gdy pole nie ma głównej cyfry)
                                        if (targetCell.value == 0) {
                                            board = board.mapIndexed { rowIdx, row ->
                                                row.mapIndexed { colIdx, cell ->
                                                    if (rowIdx == r && colIdx == c) {
                                                        val updatedNotes = if (cell.notes.contains(num)) {
                                                            cell.notes - num
                                                        } else {
                                                            cell.notes + num
                                                        }
                                                        cell.copy(notes = updatedNotes)
                                                    } else cell
                                                }
                                            }
                                        }
                                    } else {
                                        // Tryb normalny: wpisz główną cyfrę, wyczyść notatki w tym polu
                                        // oraz automatycznie usuń tę cyfrę z notatek w tym samym wierszu, kolumnie i bloku 3x3
                                        board = board.mapIndexed { rowIdx, row ->
                                            row.mapIndexed { colIdx, cell ->
                                                if (rowIdx == r && colIdx == c) {
                                                    cell.copy(value = num, notes = emptySet())
                                                } else if (rowIdx == r || colIdx == c || (rowIdx / 3 == r / 3 && colIdx / 3 == c / 3)) {
                                                    cell.copy(notes = cell.notes - num)
                                                } else {
                                                    cell
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isNoteMode) Color(0xFF455A64) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(text = num.toString(), fontSize = 20.sp)
                    }
                }
            }

            // Przyciski funkcyjne: Notatki (Ołówek), Wyczyść, Nowa gra
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Przycisk przełączania trybu notatek
                Button(
                    onClick = { isNoteMode = !isNoteMode },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isNoteMode) Color(0xFF2E7D32) else Color.LightGray,
                        contentColor = if (isNoteMode) Color.White else Color.Black
                    ),
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text(
                        text = if (isNoteMode) "✎ Notatki: WŁ" else "✎ Notatki: WYŁ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Przycisk czyszczenia pola (usuwa główną cyfrę oraz notatki)
                OutlinedButton(
                    onClick = {
                        selectedCell?.let { (r, c) ->
                            if (!board[r][c].isInitial && !isSolved) {
                                board = board.mapIndexed { rowIdx, row ->
                                    row.mapIndexed { colIdx, cell ->
                                        if (rowIdx == r && colIdx == c) {
                                            cell.copy(value = 0, notes = emptySet())
                                        } else cell
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Wyczyść", fontSize = 13.sp)
                }

                // Przycisk nowej gry
                Button(
                    onClick = {
                        board = generateSudoku(difficulty)
                        selectedCell = null
                    },
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("Nowa gra", fontSize = 13.sp)
                }
            }
        }
    }
}

// =========================================================================
// LOGIKA GENEROWANIA PLANSZY SUDOKU
// =========================================================================

fun generateSudoku(difficulty: Difficulty): List<List<Cell>> {
    val grid = Array(9) { IntArray(9) }

    for (i in 0 until 9 step 3) {
        val nums = (1..9).shuffled()
        var idx = 0
        for (r in 0..2) {
            for (c in 0..2) {
                grid[i + r][i + c] = nums[idx++]
            }
        }
    }

    solveBoard(grid)
    val solution = Array(9) { r -> grid[r].clone() }

    var removed = 0
    while (removed < difficulty.emptyCells) {
        val r = Random.nextInt(9)
        val c = Random.nextInt(9)
        if (grid[r][c] != 0) {
            grid[r][c] = 0
            removed++
        }
    }

    return List(9) { r ->
        List(9) { c ->
            Cell(
                row = r,
                col = c,
                value = grid[r][c],
                solution = solution[r][c],
                isInitial = grid[r][c] != 0
            )
        }
    }
}

private fun solveBoard(grid: Array<IntArray>): Boolean {
    for (r in 0..8) {
        for (c in 0..8) {
            if (grid[r][c] == 0) {
                for (num in 1..9) {
                    if (isValidPlacement(grid, r, c, num)) {
                        grid[r][c] = num
                        if (solveBoard(grid)) return true
                        grid[r][c] = 0
                    }
                }
                return false
            }
        }
    }
    return true
}

private fun isValidPlacement(grid: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
    for (i in 0..8) {
        if (grid[row][i] == num || grid[i][col] == num) return false
    }
    val startRow = (row / 3) * 3
    val startCol = (col / 3) * 3
    for (r in 0..2) {
        for (c in 0..2) {
            if (grid[startRow + r][startCol + c] == num) return false
        }
    }
    return true
}