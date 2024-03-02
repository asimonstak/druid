SELECT 
    MAX(MIN(c5)) OVER(PARTITION BY c8 ORDER BY c1 RANGE BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW), c8 
from ( SELECT * from "t_alltype.parquet" where c5 IS NOT NULL) GROUP BY c1, c8
