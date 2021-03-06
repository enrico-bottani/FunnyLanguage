package parser;

import tokenizer.*;
import tokenizer.exceptions.CommentNotClosedException;
import tokenizer.exceptions.UnaspectedTokenException;
import tokenizer.exceptions.StringNotClosedException;
import tokenizer.exceptions.TokenizerException;

import java.io.IOException;
import java.util.ArrayList;

public class Parser {
    Tokenizer tokenizer;
    Token token;

    public Parser(Tokenizer tokenizer) throws IllegalArgumentException {
        if (tokenizer == null) throw new IllegalArgumentException("Tokenizer can't be null");
        this.tokenizer = tokenizer;
    }

    public Expr compile() throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        try {
            Expr programm = program();
            if (token.type != Token.TYPE.EOS) {
                System.out.println("Compiler error, ");
                System.out.println("TOKEN: " + token.type);
            }
            return programm;
        } catch (Exception e) {
            tokenizer.printTokenTrace();
            return null;
        }
    }

    public void genTest() throws IOException, TokenizerException {
        while (tokenizer.nextToken().type != Token.TYPE.EOS) {
        }
        tokenizer.generateTest();
    }

    public Expr program() throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        token = tokenizer.nextToken();
        FunExpr firstExpression = function(null);
        return new InvokeExpr(firstExpression, null);
    }

    // function ::= "{" optParams optLocals optSequence "}" .
    public FunExpr function(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        if (token.getType() != Token.TYPE.CURLY_BRACKET_OPEN)
            throw new UnaspectedTokenException(token.getType().toString(), Token.TYPE.CURLY_BRACKET_OPEN.toString());
        else token = tokenizer.nextToken();

        ArrayList<String> params = optParams();
        ArrayList<String> locals = optLocals();
        ArrayList<String> temps = new ArrayList<>(params);

        temps.addAll(locals);
        FunExpr funExpr = new FunExpr(params, locals, optSequence(new Scope(temps, scope)));

        if (token.getType() != Token.TYPE.CURLY_BRACKET_CLOSE)
            throw new UnaspectedTokenException(Token.TYPE.CURLY_BRACKET_CLOSE.toString(), token.getType().toString());
        else token = tokenizer.nextToken();

        return funExpr;
        //return new FunExpr(params, locals, code);
    }


    // optParams ::= ( "(" optIds ")" )? .
    private ArrayList<String> optParams() throws IOException, TokenizerException, UnaspectedTokenException {
        ArrayList<String> ids = new ArrayList<>();
        if (token.value.equals("(")) {
            token = tokenizer.nextToken();
            ids.addAll(optIds());
            if (token.type != Token.TYPE.ROUND_BRACKET_CLOSE)
                throw new UnaspectedTokenException(")", token.value);
            token = tokenizer.nextToken();
        }
        return ids;
    }

    // optLocals ::= optIds .
    private ArrayList<String> optLocals() throws TokenizerException, IOException {
        return optIds();
    }

    // optSequence ::= ( "->" sequence )? .
    private Expr optSequence(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {

        if (token.type == Token.TYPE.LAMBDA) {
            token = tokenizer.nextToken();
            return sequence(scope);
        } else if (token.type == Token.TYPE.CURLY_BRACKET_CLOSE) return null;
        else throw new UnaspectedTokenException("->", token.getType().toString());
    }

    // sequence ::= optAssignment ( ";" optAssignment )* .
    private Expr sequence(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        SeqExpr seqExpr = new SeqExpr();
        seqExpr.add(optAssignement(scope));

        while (token.type == Token.TYPE.SEMICOLON) {
            token = tokenizer.nextToken();
            if (token.type == Token.TYPE.CURLY_BRACKET_CLOSE) {
                return seqExpr;
            }
            seqExpr.add(optAssignement(scope));
        }
        return seqExpr;
    }

    // optAssignment := assignment?
    private Expr optAssignement(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        //TODO: Opt assignement può cominciare con una parentesi aperta
        // IO ho accesso ad un assignement
        // Id plus minus not num false true nil string openbrace openparent if ifnot while whilenot print println

        if (token.getType() == Token.TYPE.CURLY_BRACKET_CLOSE) {
            return null;
        }
        return assignement(scope);
    }

    // optIds::= id* .
    private ArrayList<String> optIds() throws TokenizerException, IOException {
        ArrayList<String> ids = new ArrayList<>();
        while (token.type == Token.TYPE.VARIABLE) {
            ids.add(token.getStringVal());
            token = tokenizer.nextToken();
        }
        return ids;
    }

    //  assignment ::= Id ( "=" | "+=" | "-=" | "*=" | "/=" | "%=" ) assignment
    //	  | logicalOr .
    private Expr assignement(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        //scope.checkIfScope();
        if (token.type == Token.TYPE.VARIABLE) {
            Token tokenbk = token;
            token = tokenizer.nextToken();
            switch (token.type) {
                case EQUAL:
                    token = tokenizer.nextToken();
                    if (!scope.isInScope(tokenbk.getStringVal()))
                        throw new UnexpectedSymbolException("Unexpected symbol " + token.getStringVal());
                    return new SetVarExpr(tokenbk.getStringVal(), assignement(scope), Token.TYPE.EQUAL);
                case PLUS_EQUAL:
                    token = tokenizer.nextToken();
                    if (!scope.isInScope(tokenbk.getStringVal()))
                        throw new UnexpectedSymbolException("Unexpected symbol " + token.getStringVal());
                    return new SetVarExpr(tokenbk.getStringVal(), assignement(scope), Token.TYPE.PLUS);
                case MINUS_EQUAL:
                    token = tokenizer.nextToken();
                    if (!scope.isInScope(tokenbk.getStringVal()))
                        throw new UnexpectedSymbolException("Unexpected symbol " + token.getStringVal());
                    return new SetVarExpr(tokenbk.getStringVal(), assignement(scope), Token.TYPE.MINUS);
                default:
                    tokenizer.undoNext();
                    token = tokenbk;
                    break;
            }
        }
        return logicalOr(scope);
    }

    // logicalOr ::= logicalAnd ( "||" logicalOr )? .
    // TODO
    private Expr logicalOr(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        Expr left = logicalAnd(scope);
        Token.TYPE type = token.type;
        if (type == Token.TYPE.LOGICAL_OR) {
            do {
                token = tokenizer.nextToken();
                return new OrExpr(left, logicalOr(scope));
            } while ((type = token.type) == Token.TYPE.LOGICAL_OR);
        }
        return left;
    }

    // logicalAnd ::= equality ( "&&" logicalAnd )? .
    private Expr logicalAnd(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        Expr left = equality(scope);
        Token.TYPE type = token.type;
        if (type == Token.TYPE.LOGICAL_AND) {
            do {
                token = tokenizer.nextToken();
                return new AndExpr(left, logicalAnd(scope));
            } while ((type = token.type) == Token.TYPE.LOGICAL_AND);
        }
        return left;
    }

    // equality ::= comparison ( ( "==" | "!=" ) comparison )? .
    private Expr equality(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {

        return comparison(scope);
    }

    // comparison ::= assign ( ( "<" | "<=" | ">" | ">=" ) assign )? .
    private Expr comparison(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        Expr expr = add(scope);
        Token.TYPE type = token.type;
        switch (type) {
            case MINOR:
                token = tokenizer.nextToken();
                expr = new BinaryExpr(expr, add(scope), type);
                break;
            case MAJOR:
                token = tokenizer.nextToken();
                expr = new BinaryExpr(expr, add(scope), type);
                break;
            case MAJOR_EQUAL:
                token = tokenizer.nextToken();
                expr = new BinaryExpr(expr, add(scope), type);
                break;
            case MINOR_EQUAL:
                token = tokenizer.nextToken();
                expr = new BinaryExpr(expr, add(scope), type);
                break;
            case EQUAL_EQUAL:
                token = tokenizer.nextToken();
                expr = new BinaryExpr(expr, add(scope), type);
                break;
        }
        return expr;
    }

    // assign ::= mult ( ( "+" | "-" ) mult )* .
    private Expr add(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        Expr expr = mult(scope);
        Token.TYPE type = token.type;
        if (type == Token.TYPE.PLUS || type == Token.TYPE.MINUS)
            do {
                switch (type) {
                    case PLUS:
                        token = tokenizer.nextToken();
                        expr = new BinaryExpr(expr, mult(scope), type);
                        break;
                    case MINUS:
                        token = tokenizer.nextToken();
                        expr = new BinaryExpr(expr, mult(scope), type);
                        break;

                }
            } while ((type = token.type) == Token.TYPE.PLUS || type == Token.TYPE.MINUS);
        return expr;
    }

    // mult ::= unary ( ( "*" | "/" | "%" ) unary )* .
    private Expr mult(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        Expr expr = unary(scope);
        Token bkToken = token;

        while (bkToken.getType() == Token.TYPE.ABSTERISC || bkToken.getType() == Token.TYPE.DIVIDE || bkToken.getType() == Token.TYPE.PERCENT) {
            token = tokenizer.nextToken();
            expr = new BinaryExpr(expr, unary(scope), bkToken.getType());
            bkToken = token;
        }
        return expr;
    }

    // unary ::= ( "+" | "-" | "!" ) unary
    //	| postfix .
    private Expr unary(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        switch (token.type) {
            case PLUS:
                return unary(scope);
            case MINUS:
                token = tokenizer.nextToken();
                return new UnaryExpr(unary(scope), Token.TYPE.MINUS);
            case BANG:
                return unary(scope);
            default:
                return postfix(scope);
        }

    }

    // postfix ::= primary args*
    private Expr postfix(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        Expr primary = primary(scope);
        // È Una chiamata a funzione
        if (token.getType() == Token.TYPE.ROUND_BRACKET_OPEN) {
            // TODO: Usare do while
            while (token.getType() == Token.TYPE.ROUND_BRACKET_OPEN) {
                ExprList args = args(scope);
                primary = new InvokeExpr(primary, args);
            }
        }
        return primary;
    }

    private Expr primary(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        switch (token.getType()) {
            case NUMBER:
                return num(scope);
            case STRING:
                return string(scope);
            case TRUE:
                return bool(scope);
            case FALSE:
                return bool(scope);
            case NIL:
                return nil(scope);
            case VARIABLE:
                return getId(scope);
            case CURLY_BRACKET_OPEN:
                return function(scope);
            case ROUND_BRACKET_OPEN:
                return subsequence(scope);
            case IF:
                return cond(scope);
            case WHILE:
                return loop(scope);
            case WHILENOT:
                return loop(scope);
            case PRINT:
                return print(scope);
            case PRINTLN:
                return print(scope);
            default:
                throw new UnaspectedTokenException("PRIMARY", token.getType().toString());
        }

    }

    private Expr nil(Scope scope) throws IOException, TokenizerException {
        token = tokenizer.nextToken();
        return NilVal.instance();
    }

    private Expr subsequence(Scope scope) throws IOException, TokenizerException, UnaspectedTokenException, UnexpectedSymbolException {
        assertAndUpdateToken(Token.TYPE.ROUND_BRACKET_OPEN, token.getType());
        Expr rtn = sequence(scope);
        assertAndUpdateToken(Token.TYPE.ROUND_BRACKET_CLOSE, token.getType());
        return rtn;
    }

    private Expr cond(Scope scope) throws UnexpectedSymbolException, TokenizerException, UnaspectedTokenException, IOException {
        if (token.getType() == Token.TYPE.IF || token.getType() == Token.TYPE.IFNOT) {
            token = tokenizer.nextToken();

            Expr condition = sequence(scope);
            if (token.getType() != Token.TYPE.THEN)
                throw new UnaspectedTokenException(Token.TYPE.THEN.toString(), token.getType().toString());
            token = tokenizer.nextToken();
            Expr thenDo = sequence(scope);
            Expr elseDo = null;
            if (token.getType() == Token.TYPE.ELSE) {
                token = tokenizer.nextToken();
                elseDo = sequence(scope);
            }

            if (token.getType() != Token.TYPE.FI) throw new UnexpectedSymbolException(token.type.toString());
            token = tokenizer.nextToken();

            return new IfExpr(condition, thenDo, elseDo);
        } else throw new UnexpectedSymbolException("Ciao" + token.type.toString());
    }

    private Expr loop(Scope scope) throws UnexpectedSymbolException, TokenizerException, UnaspectedTokenException, IOException {
        if (token.getType() == Token.TYPE.WHILE || token.getType() == Token.TYPE.WHILENOT) {
            boolean negate = false;
            if (token.getType() == Token.TYPE.WHILENOT) negate = true;

            token = tokenizer.nextToken();

            Expr condition = sequence(scope);
            Expr thenDo = null;
            if (token.getType() == Token.TYPE.DO) {
                token = tokenizer.nextToken();
                thenDo = sequence(scope);
            }

            if (token.getType() != Token.TYPE.OD) throw new UnexpectedSymbolException(token.type.toString());
            token = tokenizer.nextToken();

            return new WhileExpr(condition, thenDo, negate);
        } else throw new UnexpectedSymbolException("WHILENOT EXPECTED");
    }

    private Expr getId(Scope scope) throws TokenizerException, IOException, UnexpectedSymbolException {
        //TODO: Controllare se esiste nello scope
        if (!scope.isInScope(token.getStringVal())) {
            throw new UnexpectedSymbolException("[COMPILER] Unexpected symbol " + token.getStringVal());
        }
        Expr getVarExpr = new GetVarExpr(token.value);
        token = tokenizer.nextToken();
        return getVarExpr;
    }

    private Expr num(Scope scope) throws TokenizerException, IOException {
        NumVal numVal = new NumVal(token.bigDecimal);
        token = tokenizer.nextToken();
        return numVal;
    }

    private Expr bool(Scope scope) throws TokenizerException, IOException {
        BoolVal boolVal = new BoolVal(token.getType());
        token = tokenizer.nextToken();
        return boolVal;
    }

    private StringVal string(Scope scope) throws TokenizerException, IOException {
        String rtn = token.value;
        token = tokenizer.nextToken();
        return new StringVal(rtn);
    }

    private PrintExpr print(Scope scope) throws IOException, UnaspectedTokenException, TokenizerException, UnexpectedSymbolException {
        if (token.type == Token.TYPE.PRINT || token.type == Token.TYPE.PRINTLN) {
            // TODO: TENERE PRINT O PRINTLN
            Token prevToken = token;
            token = tokenizer.nextToken();
            return new PrintExpr(prevToken.getType(), args(scope));
        } else throw new UnaspectedTokenException("print or println", token.value);
    }

    // args ::= "(" ( sequence ( "," sequence )* )? ")" .
    private ExprList args(Scope scope) throws UnaspectedTokenException, IOException, TokenizerException, UnexpectedSymbolException {
        assertAndUpdateToken(Token.TYPE.ROUND_BRACKET_OPEN, token.getType());

        ExprList expressions = new ExprList();
        if (token.getType() != Token.TYPE.ROUND_BRACKET_CLOSE) {
            expressions.add(sequence(scope));
            // SE non vi è una virgola o un'altra espressione...
            while (token.getType() == Token.TYPE.COMMA) {
                token = tokenizer.nextToken();
                expressions.add(sequence(scope));
            }
        }
        // Consumo la parentesi
        assertAndUpdateToken(Token.TYPE.ROUND_BRACKET_CLOSE, token.getType());

        return expressions;

    }

    private void assertAndUpdateToken(Token.TYPE expected, Token.TYPE actual) throws TokenizerException, IOException, UnaspectedTokenException {
        if (actual == expected) {
            token = tokenizer.nextToken();
        } else throw new UnaspectedTokenException(expected.toString(), actual.toString());
    }
}
