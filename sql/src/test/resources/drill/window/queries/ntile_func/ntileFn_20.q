SELECT col7 , col9 , NTILE(3) OVER (PARTITION by col7 ORDER by col9) tile FROM "allTypsUniq.parquet"