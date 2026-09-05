# 「可复用训练方案」数据模型设计（v1）

> 目的：把「健身计划」做成**可复用的训练方案**，一套方案含若干「分节」，任一节可一键应用到某一天（今日）执行；执行记录驱动**渐进加重建议**。UI 后做，本模型先定，避免返工。
> 关键思路：**方案是模板（只存设计）**；**日期是实例（沿用现有 ScheduledExercise）**；用一条「映射」记住某天用的是哪节的哪个动作，以便回填真实重量→算建议。

## 一、需要表达的三层
```
Program（一套方案，如“四分化 减脂版”）
  └─ ProgramSession（一节，如 Day1 胸+三头）   —— 有顺序
        └─ ProgramItem（该节里一个动作及其组次数）—— 引用动作库
日期(具体某天执行) → 把某个 ProgramSession 的动作实例化到那天(现有 ScheduledExercise)
```

## 二、实体（Room）
### 1) Program
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long PK | |
| name | String | 方案名，如“四分化-减脂版” |
| goal | String? | 目标标签（减脂/增肌…） |
| dayCount | Int | 一节一轮几天（=会话数） |
| note | String? | |
| createdAt | Long | |

### 2) ProgramSession
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long PK | |
| programId | Long FK→Program | onDelete 级联 |
| order | Int | 第几节（Day1=1…） |
| name | String | 如“胸+三头” |
| createdAt | Long | |

### 3) ProgramItem
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long PK | |
| sessionId | Long FK→ProgramSession | 级联 |
| exerciseId | Long FK→Exercise | 引用动作库（若动作被删，本级联删；用前端软处理提示） |
| order | Int | 该节内顺序 |
| targetSets | Int | 正式组数 |
| repMin / repMax | Int | 目标次数区间（8/10） |
| weightKg | Double? | 起始重量；null=自重 |
| restSeconds | Int | 组间休息 |
| formCue | String? | 要领提醒（自由重量复合动作） |
| enableProgressive | Boolean | 是否参与渐进加重（默认 true） |
| note | String? | |

### 4) ProgramDayApply（某天用了方案哪一节 —— 映射，轻量）
| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long PK | |
| dateEpochDay | Long | 唯一约束（一天只映射一节的推荐） |
| programId | Long FK | 冗余，便于查询 |
| sessionId | Long FK→ProgramSession | |
| appliedAt | Long | |

> 行为：把 ProgramSession 应用到 dateEpochDay 时：
> ① 先清空该天已有 ScheduledExercise；
> ② 依 ProgramItem 生成 ScheduledExercise（复制组/次/重量/休息/要领到备注）；
> ③ 写入 ProgramDayApply(dateEpochDay, sessionId)。
> 之后在“今日”里可自由增删改该天条目（实例不改模板）。

### 5) 渐进加重：执行基线（用于给“下次建议”）
> 选项 A（推荐、轻量）：以**单个动作**为粒度记录“最近一次主项完成的最好表现”。
| 字段 | 类型 | 说明 |
|---|---|---|
| exerciseId | Long PK(=FK Exercise) | |
| bestWeightKg | Double? | 该动作最近达到的重量 |
| bestReps | Int? | 该重量下的最好次数 |
| suggestedWeightKg | Double? | 依据规则算出的“下次建议” |
| updatedAt | Long | |

规则（规则引擎函数 nextWeight(repMin,repMax,bestReps,bestWeightKg)）：
- bestReps ≥ repMax → suggested = bestWeightKg + 2.5（哑铃小重量 +1）
- 中段 → suggested = bestWeightKg
- bestReps ≤ repMin 或连续不佳 → suggested = bestWeightKg*0.95 向下取整
- 手动可覆盖 suggestedWeightKg（把“手动决定”也落库，避免自动改）

> 选项 B（更精确、二期）：记录每次执行的每个“组”的 (重量×次数) 成 SetLog 表 → 建议更准，但要额外录入。MVP 先不做，模型预留字段。

## 三、与现有表的关系（不破坏现有功能）
- ScheduledExercise 已承担“某天计划条目/打勾”职责 → **不加字段，避免破坏**。
- 复用 Exercise 动作库（新增四分化需要的动作若库里没有，先在动作库补或用现成的）。
- Exercise 目前每个动作 id 唯一；同一动作可出现在多个 ProgramItem。

## 四、创建流程（UI 草图，仅示意）
1. 「计划」里新建 Program（选几天/名称/目标）。
2. 逐节加动作（从动作库选 + 填 组/次数区间/起始重量/休息），可选填要领。
3. 存为模板。
4. 到某天：选 ProgramSession → 一键应用（生成当天实例）→ 执行打勾。
5. 收尾记录重量→建议下次 → 写入 ExerciseProgression。

## 五、内置种子
首次可选写入示例：把《四分化推荐版》或 U/L 版作为一套内置 Program，用户可复制/改/建自己的，降低从零搭建成本。

## 待定/可议
- 一个 Exercise 的渐进基线是“全局”还是“按方案按节”？默认全局（同动作建议统一），若你希望不同节不同重量再改。
- 是否要「每周/周期回顾」（各部位本周总量图）——影响是否补 ProgramDayApply 的周汇总查询。
