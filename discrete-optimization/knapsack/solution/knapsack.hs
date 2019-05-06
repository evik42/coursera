import Control.Monad (replicateM)
import Data.Array.IO

type Value = Int
type Weight = Int
data Artifact = Artifact Value Weight deriving Show

artifactFromTuple :: (Value, Weight) -> Artifact
artifactFromTuple (v, w) = Artifact v w

readInts :: [Char] -> (Int, Int)
readInts cs = (head p, head $ tail p)
              where
                p = map read $ words cs

readArtifact :: [Char] -> Artifact
readArtifact = artifactFromTuple . readInts

type Taken = [Int]

createTaken :: Int -> Taken -> [Char]
createTaken n is =
                  let filler i cs = take ((-) i $ length cs) $ repeat "0"
                      filled i cs = filler i cs ++ cs
                      go :: [Int] -> [[Char]] -> [[Char]]
                      go [] out = out
                      go (i:is) out = go is $ "1" : filled i out
                  in reverse $ unwords $ filled n $ go is []

type Node = (Value, Taken)

bestNode :: Int -> Artifact -> IOArray Int Node -> Int -> IO Node
bestNode n (Artifact v w) prev i
  | (i - w) < 0 = readArray prev i
  | otherwise   = do (v1, t1) <- readArray prev $ i - w
                     a2@(v2, t2) <- readArray prev i
                     return $ if v1 + v > v2 then (v1 + v, n : t1) else a2

nextCol :: Int -> (Int -> IO Node) -> IOArray Int Node -> Int -> IO ()
nextCol k best cur i
  | i > k     = return ()
  | otherwise = let node = best i
                in node `seq` node >>= writeArray cur i >> nextCol k best cur (i + 1)

printProgress :: Int -> IO ()
printProgress n
  | (n `mod` 10) == 0 = putStrLn $ "n: " ++ show n
  | otherwise         = return ()

dynamicProgramming :: Int -> [Artifact] -> IO Node
dynamicProgramming k as = let go :: [Artifact] -> IOArray Int Node -> IOArray Int Node -> Int -> Int -> IO Node
                              go [] prev _ _ _       = do (v, t) <- readArray prev k
                                                          return (v, reverse t)
                              go (a:as) prev cur k n = printProgress n >> nextCol k (bestNode n a prev) cur 0 >> go as cur prev k (n + 1)
                          in do arr1 <- newArray (0,k) (0, []) :: IO (IOArray Int Node)
                                arr2 <- newArray (0,k) (0, []) :: IO (IOArray Int Node)
                                go as arr1 arr2 k 0

main = do
  parameters <- getLine
  let (n, k) = readInts parameters
  artifacts <- replicateM n $ do
    artifact <- getLine
    return $ readArtifact artifact
  (v, arts) <- dynamicProgramming k artifacts
  putStrLn $ show v ++ " 1"
  putStrLn $ createTaken n arts
