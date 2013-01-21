import java.util.*;
import java.util.Arrays;
import java.util.ArrayList;  

/*
 * TOKENIZER CLASS
 * Main Tokenizer class that breaks input text into tokens - unigram, BIGRAM+, TRIGRAM +.
 * @author Anthony Kunnel Jose
 */
public class Tokenizer 
{
	public String scheme; 
	public String input; 

	public Tokenizer( String scheme , String input )
	{
		this.scheme = scheme;
		this.input  = input ;  
	}

	public ArrayList<String> Tokenize()
	{
		if( scheme.equals("UNIGRAM") )
		{
			return unigramTokenizer( input );
		}	
		else if( scheme.equals("BIGRAM") )	
		{
			return bigramTokenizer( input ) ;	
		}		
		else
		{
			return trigramTokenizer( input );
		}	
					
	}

	/*
	*	UNIGRAM TOKENIZER
	*	Will create a UNIGRAM ArrayList from the input string. 
	*			
	*/
	public ArrayList<String> unigramTokenizer( String input )
	{
		String[] tokens = input.trim().split("\\s+");
		ArrayList<String> result = new ArrayList<String>( Arrays.asList( tokens ) );
	
		return result ;
	}

	/*
	*	BIGRAM TOKENIZER
	*	Will create a BIGRAM+ ArrayList from the input string. 
	*	(prev->current) tuples added
	*/
	public ArrayList<String> bigramTokenizer( String input )
	{
		String[] tokens = input.trim().split("\\s+");
		ArrayList<String> result = unigramTokenizer( input );

		String prev=""; 
		String current;
		
		for( String s : tokens )
		{
			current = s;

			if( prev != "" )	
			{
				result.add( prev+" "+current );	
			}	
			
			prev = s;
		}	

		return result ;
	}

	/*
	*	TRIGRAM TOKENIZER
	*	Will create a TRIGRAM+ ArrayList from the input string. 
	*	prev2->prev1->current
	*/
	public ArrayList<String> trigramTokenizer( String input )
	{
		String[] tokens = input.trim().split("\\s+");
		ArrayList<String> result = bigramTokenizer( input );

		String prev1 = "";
		String prev2 = "";
		String current;

		for( String s : tokens )
		{
			current = s;

			if(	prev1 != "" && 
				prev2 != "" )
			{	
				result.add( prev2+" "+prev1+" "+current );	
			}	
			prev2 = prev1; 
			prev1 = current;
	
		}	
		return result ;
	}

	public static void main( String[] args )
	{
		String scheme = "BIGRAM";
		String input = "This is a test string";
		Tokenizer tk = new Tokenizer( scheme, input );
		ArrayList tokens = tk.Tokenize() ; 
	}	


}