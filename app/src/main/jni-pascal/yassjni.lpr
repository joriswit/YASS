library yassjni;

// ============================================================================
// Compiling instructions
//
// The conditional symbols in 'YASS.pas' must be edited in order to compile
// this dll version of the plugin:
//
// disable: {$DEFINE CONSOLE_APPLICATION}
// enable : {$DEFINE PLUGIN_MODULE}
//
// enable:  {$DEFINE FPC}
// disable :{$DEFINE DELPHI}
//
// disable :{$DEFINE WINDOWS}
// ============================================================================
{$MODE DELPHI}

uses SysUtils, jni, YASS;

type
  TOptimizerSearchMethodOrder = ( PVR, VPR, PVRG, VPRG );

{Global Variables}
var PEnv:PJNIEnv;
    Obj:JObject;
    SokobanStatus:TSokobanStatus;

function SokobanCallBackFunction(): Integer; stdcall;
var StatusTextAsJString:JString;
    Cls:JClass;
    MID:jmethodID;
    jParams: array[0..0] of jValue;
begin
  StatusTextAsJString:=(PEnv^^).NewStringUTF(PEnv, SokobanStatus.StatusText);
  jParams[0].l:= StatusTextAsJString;

  Cls:=(PEnv^^).GetObjectClass(PEnv, Obj);
  MID:=(PEnv^^).GetMethodID(PEnv, Cls, 'onProgress', '(Ljava/lang/String;)V');
  if MID<>nil then
     (PEnv^^).CallNonvirtualVoidMethodA(PEnv,Obj,Cls,MID,@jParams);

  (PEnv^^).DeleteLocalRef(PEnv,StatusTextAsJString);
  Result:=0;
end;

{exported functions}
function Java_yass_YASSActivity_00024SolverTask_optimize(
                 PEnv__:PJNIEnv;Obj__            : JObject;
                 Width__, Height__               : Cardinal;
                 BoardAsJString__                : JString;
                 GameAsJString__                 : JString;  // the game to optimize
                 TransPositionTableSize__        : Cardinal;
                 TimeLimitS__                    : Cardinal;
                 OptimizerSearchMethodOrder__    : TOptimizerSearchMethodOrder;
                 VicinityBox1__                  : Cardinal;
                 VicinityBox2__                  : Cardinal;
                 Optimization__                  : TOptimization
): JString; cdecl;
{ Java JNI method yass.YASSActivity$SolverTask.optimize(...) }
var Col,Row,Index:Integer;
    IsASolution:Boolean=False;
    InitializeErrorText:String;
    InitializePluginResult:TPluginResult=prOK;
    BoardAsPChar:PChar;
    BoardAsText:String;
    GameAsPChar:PChar;
    GameAsText:String;
    ResultGameAsText:PChar;
    ExceptionClass:JClass;
    ExceptionExists:JBoolean;
begin
  PEnv:=PEnv__;
  Obj:=Obj__;
  try
    BoardAsPChar:=(PEnv^^).GetStringUTFChars(PEnv, BoardAsJString__, nil);
    BoardAsText:=BoardAsPChar;
    (PEnv^^).ReleaseStringUTFChars(PEnv__, BoardAsJString__, BoardAsPChar);
    GameAsPChar:=(PEnv^^).GetStringUTFChars(PEnv, GameAsJString__, nil);
    GameAsText:=GameAsPChar;
    (PEnv^^).ReleaseStringUTFChars(PEnv__, GameAsJString__, GameAsPChar);

    SokobanStatus.Size:=SizeOf(TSokobanStatus);
    if   (Width__ >=3) and (Width__ <=MAX_BOARD_WIDTH ) and
         (Height__>=3) and (Height__<=MAX_BOARD_HEIGHT) then
         if Cardinal(Length(BoardAsText))=Width__*Height__ then
                              try
                                   Positions.MemoryByteSize:=TransPositionTableSize__*1024*1024;
                                   if   TimeLimitS__ <> 0 then
                                        Solver.SearchLimits.TimeLimitMS:=TimeLimitS__*1000
                                   else Solver.SearchLimits.TimeLimitMS:=High(Solver.SearchLimits.TimeLimitMS); {high-value signals unlimited search time}
                                   Solver.SearchLimits.PushCountLimit:=High(Solver.SearchLimits.PushCountLimit);
                                   Solver.SearchLimits.DepthLimit:=MAX_HISTORY_BOX_MOVES;
                                   Optimizer.SearchLimits.PushCountLimit:=Solver.SearchLimits.PushCountLimit;
                                   Optimizer.SearchLimits.DepthLimit:=Solver.SearchLimits.DepthLimit;
                                   Optimizer.SearchLimits.TimeLimitMS:=Solver.SearchLimits.TimeLimitMS;
                                   Solver.BackwardSearchDepthLimit:=Solver.SearchLimits.DepthLimit;
                                   Game.DeadlockSets.AdjacentOpenSquaresLimit:=1;
                                   Game.DeadlockSets.BoxLimitForDynamicSets:=DEFAULT_DEADLOCK_SETS_BOX_LIMIT_FOR_DYNAMIC_SETS;
                                   Game.DeadlockSets.BoxLimitForPrecalculatedSets:=DEFAULT_DEADLOCK_SETS_BOX_LIMIT_FOR_PRECALCULATED_SETS;
                                   Solver.SearchMethod:=smOptimize;
                                   Solver.Enabled:=False;
                                   Solver.FindPushOptimalSolution:=True;
                                   Optimizer.Enabled:=True;
                                   Solver.ShowBestPosition:=False;
                                   Solver.StopWhenSolved:=False;
                                   UserInterface.Prompt:=False;
                                   Solver.ReuseNodesEnabled:=False;
                                   LogFile.Enabled:=False;
                                   Solver.PackingOrder.Enabled:=True;
                                   Solver.PackingOrder.BoxCountThreshold:=9;
                                   Solver.SokobanCallBackFunction:=@SokobanCallBackFunction;
                                   Solver.SokobanStatusPointer:=@SokobanStatus;
                                   Optimizer.MethodEnabled   [omBoxPermutations                        ]:=False; {the fallback strategy is the "box permutations" method}
                                   Optimizer.MethodEnabled   [omRearrangement                          ]:=True;
                                   Optimizer.MethodEnabled   [omGlobalSearch                           ]:= ( OptimizerSearchMethodOrder__ = PVRG ) or ( OptimizerSearchMethodOrder__ = VPRG );
                                   Optimizer.MethodEnabled   [omBoxPermutationsWithTimeLimit           ]:=True;
                                   Optimizer.MethodEnabled   [omVicinitySearch                         ]:=True;

                                   Optimizer.MethodOrder     [Low(Optimizer.MethodOrder)               ]:=Low(TOptimizationMethod); {the fallback method is the first one}
                                   if ( OptimizerSearchMethodOrder__ = PVR ) or ( OptimizerSearchMethodOrder__ = PVRG) then begin
                                     Optimizer.MethodOrder     [2]:=omRearrangement;
                                     Optimizer.MethodOrder     [4]:=omGlobalSearch;
                                     Optimizer.MethodOrder     [1]:=omBoxPermutationsWithTimeLimit;
                                     Optimizer.MethodOrder     [3]:=omVicinitySearch;
                                   end
                                   else begin
                                     Optimizer.MethodOrder     [3]:=omRearrangement;
                                     Optimizer.MethodOrder     [4]:=omGlobalSearch;
                                     Optimizer.MethodOrder     [2]:=omBoxPermutationsWithTimeLimit;
                                     Optimizer.MethodOrder     [1]:=omVicinitySearch;
                                   end;

                                   Optimizer.Optimization:=Optimization__;

                                   with Optimizer do begin
                                     FillChar(VicinitySettings,SizeOf(VicinitySettings),0);
                                     Index:=MAX_VICINITY_BOX_COUNT;
                                     if VicinityBox1__>=1 then begin VicinitySettings[Index]:=VicinityBox1__; Dec(Index); end;
                                     if VicinityBox2__>=1 then begin VicinitySettings[Index]:=VicinityBox2__; Dec(Index); end;
                                     QuickVicinitySearchEnabled:=True;
                                     end;

                                   if YASS.Initialize(
                                        Positions.MemoryByteSize,
                                        Solver.SearchLimits.PushCountLimit,
                                        Solver.SearchLimits.DepthLimit,
                                        Solver.BackwardSearchDepthLimit,
                                        Optimizer.SearchLimits.PushCountLimit,
                                        Optimizer.SearchLimits.DepthLimit,
                                        Game.DeadlockSets.AdjacentOpenSquaresLimit,
                                        Game.DeadlockSets.BoxLimitForDynamicSets,
                                        Game.DeadlockSets.BoxLimitForPrecalculatedSets,
                                        Solver.SearchMethod,
                                        Solver.Enabled,
                                        Optimizer.Enabled,
                                        Optimizer.QuickVicinitySearchEnabled,
                                        Solver.ShowBestPosition,//False,
                                        Solver.StopWhenSolved,
                                        UserInterface.Prompt,
                                        Solver.ReuseNodesEnabled,
                                        LogFile.Enabled,
                                        Solver.PackingOrder.Enabled,
                                        Solver.PackingOrder.BoxCountThreshold,
                                        Solver.SearchLimits.TimeLimitMS,
                                        Optimizer.SearchLimits.TimeLimitMS,
                                        Optimizer.BoxPermutationsSearchTimeLimitMS,
                                        Optimizer.MethodEnabled,
                                        Optimizer.MethodOrder,
                                        Optimizer.Optimization,
                                        Optimizer.VicinitySettings,
                                        Solver.SokobanCallBackFunction,
                                        Solver.SokobanStatusPointer) then begin

                                      InitializeBoard(Width__,Height__,True);
                                      for Row:=1 to Game.BoardHeight do
                                          for Col:=1 to Game.BoardWidth do begin
                                              Game.Board[ColRowToSquare(Col,Row)]:=Legend.CharToItem[BoardAsText[(Row-1)*Game.BoardWidth+Col]];
                                              end;

                                      if InitializeGame(InitializePluginResult,InitializeErrorText) then begin
                                         //MessageBox(Format('Width: %d, Height: %d',[Game.BoardWidth,Game.BoardHeight])+NL+BoardToText(NL),Game.Title,MB_ICONINFORMATION);

                                         if OptimizeGame(Length(GameAsText),PChar(GameAsText)) then begin
                                            if   (Length(GameAsText)<>0) then begin
                                                 ResultGameAsText:=StrAlloc(MAX_HISTORY_PLAYER_MOVES);
                                                 PathToText(Positions.BestPosition,MAX_HISTORY_PLAYER_MOVES,ResultGameAsText,IsASolution,Optimizer.Result);
                                                 //Result:=Ord(Optimizer.Result);
                                                 Result:=(PEnv^^).NewStringUTF(PEnv, ResultGameAsText)
                                                 end
                                            else Result:=nil; // the caller didn't provide a buffer for the game: report back that the level (or rather the game) is invalid
                                            end
                                         else Result:=nil;
                                         end
                                      else Result:=nil;
                                      end
                                   else Result:=nil;
                              finally YASS.Finalize; // the plugin can and must be finalized, even if 'YASS.Initialize' failed
                              end
         else
            Result:=nil
    else Result:=nil;
  except
    on E: Exception do begin
      Result:=nil;
      ExceptionClass:=(PEnv^^).FindClass(PEnv, 'java/lang/Exception');
      ExceptionExists:=(PEnv^^).ExceptionCheck(PEnv);
      if ExceptionExists <> 0 then
          (PEnv^^).ExceptionClear(PEnv);
      (PEnv^^).ThrowNew(PEnv, ExceptionClass, PChar(E.Message));
      end;
  end
end;

function Java_yass_YASSActivity_00024SolverTask_solve(
                 PEnv__:PJNIEnv;Obj__            : JObject;
                 Width__,Height__                : Cardinal;
                 BoardAsJString__                : JString;
                 TransPositionTableSize__        : Cardinal;
                 TimeLimitS__                    : Cardinal
                 ): JString; cdecl;
{ Java JNI method yass.YASSActivity$SolverTask.solve(...) }
var Col,Row:Integer; InitializeErrorText:String;
    InitializePluginResult:TPluginResult;
    BoardAsPChar:PChar;
    BoardAsText:String;
    ExceptionClass:JClass;
    ExceptionExists:JBoolean;
begin
  PEnv:=PEnv__;
  Obj:=Obj__;
  try
    BoardAsPChar:=(PEnv^^).GetStringUTFChars(PEnv, BoardAsJString__, nil);
    BoardAsText:=BoardAsPChar;
    (PEnv^^).ReleaseStringUTFChars(PEnv__, BoardAsJString__, BoardAsPChar);

    SokobanStatus.Size:=SizeOf(TSokobanStatus);
    if   (Width__ >=3) and (Width__ <=MAX_BOARD_WIDTH ) and
         (Height__>=3) and (Height__<=MAX_BOARD_HEIGHT) then
       if Cardinal(Length(BoardAsText))=Width__*Height__ then
          try
               Positions.MemoryByteSize:=TransPositionTableSize__*1024*1024;
               if   TimeLimitS__ <> 0 then
                    Solver.SearchLimits.TimeLimitMS:=TimeLimitS__*1000
               else Solver.SearchLimits.TimeLimitMS:=High(Solver.SearchLimits.TimeLimitMS); {high-value signals unlimited search time}
               Solver.SearchLimits.PushCountLimit:=High(Solver.SearchLimits.PushCountLimit);
               Solver.SearchLimits.DepthLimit:=MAX_HISTORY_BOX_MOVES;
               Optimizer.SearchLimits.PushCountLimit:=Solver.SearchLimits.PushCountLimit;
               Optimizer.SearchLimits.DepthLimit:=Solver.SearchLimits.DepthLimit;
               Optimizer.SearchLimits.TimeLimitMS:=Solver.SearchLimits.TimeLimitMS;
               Solver.BackwardSearchDepthLimit:=Solver.SearchLimits.DepthLimit;
               Game.DeadlockSets.AdjacentOpenSquaresLimit:=1;
               Game.DeadlockSets.BoxLimitForDynamicSets:=DEFAULT_DEADLOCK_SETS_BOX_LIMIT_FOR_DYNAMIC_SETS;
               Game.DeadlockSets.BoxLimitForPrecalculatedSets:=DEFAULT_DEADLOCK_SETS_BOX_LIMIT_FOR_PRECALCULATED_SETS;
               Solver.SearchMethod:=TSearchMethod.smPerimeter;
               Solver.Enabled:=True;
               Solver.FindPushOptimalSolution:=True;
               Optimizer.Enabled:=True;
               Optimizer.QuickVicinitySearchEnabled:=True;
               Solver.ShowBestPosition:=False;
               Solver.StopWhenSolved:=True;
               UserInterface.Prompt:=False;
               Solver.ReuseNodesEnabled:=False;
               LogFile.Enabled:=False;
               Solver.PackingOrder.Enabled:=True;
               Solver.PackingOrder.BoxCountThreshold:=9;
               Solver.SokobanCallBackFunction:=@SokobanCallBackFunction;
               Solver.SokobanStatusPointer:=@SokobanStatus;
               //Optimizer.Optimization:=Optimization;

               if YASS.Initialize(
                    Positions.MemoryByteSize,
                    Solver.SearchLimits.PushCountLimit,
                    Solver.SearchLimits.DepthLimit,
                    Solver.BackwardSearchDepthLimit,
                    Optimizer.SearchLimits.PushCountLimit,
                    Optimizer.SearchLimits.DepthLimit,
                    Game.DeadlockSets.AdjacentOpenSquaresLimit,
                    Game.DeadlockSets.BoxLimitForDynamicSets,
                    Game.DeadlockSets.BoxLimitForPrecalculatedSets,
                    Solver.SearchMethod,
                    Solver.Enabled,
                    Optimizer.Enabled,
                    Optimizer.QuickVicinitySearchEnabled,
                    Solver.ShowBestPosition,//False,
                    Solver.StopWhenSolved,
                    UserInterface.Prompt,
                    Solver.ReuseNodesEnabled,
                    LogFile.Enabled,
                    Solver.PackingOrder.Enabled,
                    Solver.PackingOrder.BoxCountThreshold,
                    Solver.SearchLimits.TimeLimitMS,
                    Optimizer.SearchLimits.TimeLimitMS,
                    Optimizer.BoxPermutationsSearchTimeLimitMS,
                    Optimizer.MethodEnabled,
                    Optimizer.MethodOrder,
                    Optimizer.Optimization,
                    Optimizer.VicinitySettings,
                    Solver.SokobanCallBackFunction,
                    Solver.SokobanStatusPointer) then begin

                  InitializeBoard(Width__,Height__,True);

                  for Row:=1 to Game.BoardHeight do
                      for Col:=1 to Game.BoardWidth do begin
                          Game.Board[ColRowToSquare(Col,Row)]:=Legend.CharToItem[BoardAsText[(Row-1)*Game.BoardWidth+Col]];
                          end;

                  InitializePluginResult:=prOK;
                  if InitializeGame(InitializePluginResult,InitializeErrorText) then begin
                     if Search then
                        Result:=(PEnv^^).NewStringUTF(PEnv, PChar(GameHistoryMovesAsText))
                     else
                        Result:=nil;
                  end
                  else Result:=nil
                  end
               else Result:=nil;
          finally YASS.Finalize; // the plugin can and must be finalized, even if 'YASS.Initialize' failed
          end
       else
          Result:=nil
    else Result:=nil;
  except
    on E: Exception do begin
      Result:=nil;
      ExceptionClass:=(PEnv^^).FindClass(PEnv, 'java/lang/Exception');
      ExceptionExists:=(PEnv^^).ExceptionCheck(PEnv);
      if ExceptionExists <> 0 then
          (PEnv^^).ExceptionClear(PEnv);
      (PEnv^^).ThrowNew(PEnv, ExceptionClass, PChar(E.Message));
      end;
  end
end;

procedure Java_yass_YASSActivity_00024SolverTask_terminate(
                 PEnv__ : PJNIEnv;
                 Obj__  : JObject
                 ); cdecl;
{ Java JNI method yass.YASSActivity$SolverTask.terminate() }
begin
  YASS.Terminate;
end;

exports
  Java_yass_YASSActivity_00024SolverTask_optimize,
  Java_yass_YASSActivity_00024SolverTask_solve,
  Java_yass_YASSActivity_00024SolverTask_terminate;
begin
end.
