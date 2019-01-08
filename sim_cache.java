import java.io.File;
import java.text.DecimalFormat;
import java.util.Scanner;

public class sim_cache {
	
	String[][] cache;
	int[][] valid_bits;
	int[][] counter;
	private static int Write_miss_countL1=0,Read_Miss_countL1=0,Write_miss_countL2=0,Read_Miss_countL2=0,Write_countL2,Read_countL2,Read_countL1,Write_countL1;
	private static int sel,set=0,count=0,Swap_request=0,Swap_count=0,Writeback_L2=0,Memory_traffic=0;
	double Num_index_bits;
	double Num_block_offset_bits;
	double Num_tag_bits;
	public static int L2_assoc,Vc_num_blocks;
	String Tag_bits;
	int Index_bits,sets,vc;
	private static int select=0,Writeback_count=0;
	private static sim_cache L2;
	private static sim_cache L1;
	private static sim_cache VC;
	private static char chars;
	private static boolean call,Hit_VC=false,flag;
	public void values(int size,int assoc,int Blocksize)
	{
		this.sets=(size)/(assoc*Blocksize);
		this.Num_index_bits=logb(sets,2);
		this.Num_block_offset_bits=logb(Blocksize,2);
		this.Num_tag_bits=32-(Num_index_bits+Num_block_offset_bits);
		this.cache= new String[sets][2*assoc];
		this.valid_bits=new int[sets][assoc];
		this.counter=new int[sets][assoc];
		if (set==1)
			this.vc=1;
		if (Blocksize==assoc)
		{
			this.vc=1;
		}
	}	
	public static void main(String[] args) throws Exception
	{	
		int Blocksize,L1_size,L1_assoc,L2_size;
		String trace_file;
		
		Blocksize=Integer.parseInt(args[0]);
		L1_size=Integer.parseInt(args[1]);
		L1_assoc=Integer.parseInt(args[2]);
		Vc_num_blocks=Integer.parseInt(args[3]);
		L2_size=Integer.parseInt(args[4]);
		L2_assoc=Integer.parseInt(args[5]);
		trace_file=args[6];
		L1= new sim_cache();
		L2= new sim_cache();
		VC=new sim_cache();
		L1.values(L1_size, L1_assoc, Blocksize);
		if(L2_size!=0)
		{	sel=1;
			L2.values(L2_size, L2_assoc, Blocksize);
		}	
		if (Vc_num_blocks!=0)
		{
			set=1;
			VC.values((Vc_num_blocks*Blocksize),Vc_num_blocks,Blocksize);
		}	
		File file = new File(trace_file); 
		Scanner sc = new Scanner(file);	
		Config_display(Blocksize,L1_size,L1_assoc,L2_size,trace_file);
		while (sc.hasNextLine()) 
		{	     
		//while(true)			    
		//{Scanner data = new Scanner(System.in);
		String input_data=sc.nextLine();
		//String input_data=data.nextLine();
		L1.generate(input_data);
		if (chars=='r')
		{	
			call=false;
			count=0;
			Read_countL1++;
			L1.Read_cache(input_data,L1_assoc);
		}
		else if(chars=='w')
		{
			call=false;
			count=0;
			select=1;
			Write_countL1++;
			L1.Write_cache(input_data,L1_assoc);
		}
		}
		display(L1_assoc);
		
	//}
	}
	double logb(int a ,int b)
	{
		return (Math.log(a)/Math.log(b));
	}
	void Read_cache(String input_data,int assoc)
	{	
		boolean Hit=false,Invalid=false;
		int i,invalid_index=0;
		this.generate(input_data);
		for(i=0;i<assoc;i++)
		{	
			if(this.valid_bits[this.Index_bits][i]==1)
			{	String[] sub= this.cache[this.Index_bits][i].split(" ");
				if(sub[0].compareTo(this.Tag_bits)==0)
				{	
					for(int j=0;j<assoc;j++)
					{
						if(this.counter[this.Index_bits][i]>this.counter[this.Index_bits][j])
						{
							this.counter[this.Index_bits][j]++;
						}
					}
					this.counter[this.Index_bits][i]=0;
					Hit=true;
					if(call==false && count!=0)
					{	
						call=true;
					}
					break;
				}
								
			}
			else
			{
				Invalid=true;
				invalid_index=i;
				if(count!=0)
					call=true;
				break;
			}
			
		}
		if (!Hit)
		{	
			if (count==0)
			{	Read_Miss_countL1++;
			}
			if (count!=0)
			{
				call=false;
				Read_Miss_countL2++;
			}
			if (Invalid)
			{
					Invalid(this.Index_bits,invalid_index,this.Tag_bits,assoc,input_data);		
			}
			else
			{			
					Miss(this.Index_bits,this.Tag_bits,assoc,input_data);
			}
			if(call==false && count==0 && sel==1 && Hit_VC==false)
			{
				count++;
				Read_countL2++;
				L2.Read_cache(input_data,L2_assoc);
			}
		}
	}	
	void Write_cache(String input_data,int assoc)
	{
		boolean Hit=false,Invalid=false;
		int i,invalid_index=0;
		this.generate(input_data);
		for(i=0;i<assoc;i++)
		{
			if(this.valid_bits[this.Index_bits][i]==1)
			{	String[] sub= this.cache[this.Index_bits][i].split(" ");
				if(sub[0].compareTo(this.Tag_bits)==0)
				{	
					for(int j=0;j<assoc;j++)
					{
						if(this.counter[this.Index_bits][i]>this.counter[this.Index_bits][j])
						{
							this.counter[this.Index_bits][j]++;
						}
					}
					this.counter[this.Index_bits][i]=0;
					this.cache[this.Index_bits][i]=sub[0];
					this.cache[this.Index_bits][i]+=" D ";
					Hit=true;
					if(call==false && count!=0)
						call=true;
					break;
				}
			}
			else
			{
				Invalid=true;
				invalid_index=i;
				if(count!=0)
					call=true;
				break;
			}
		}
		if (!Hit)
		{	if (count==0)
				Write_miss_countL1++;
			if (count!=0)
			{	
				call=false;
				Write_miss_countL2++;
			}
			if (Invalid)
			{
				Invalid_write(this.Index_bits,invalid_index,this.Tag_bits,assoc,input_data);												
			}
			else
			{	
				Miss_write(this.Index_bits,this.Tag_bits,assoc,input_data);
			}
			if(call==false && count==0 && select!=0 && sel==1 && Hit_VC==false)
			{
				count++;
				Read_countL2++;
				L2.Read_cache(input_data,L2_assoc);
			}
		}	
	}
	public void Invalid(int Index_bits,int invalid_index,String Tag_bits,int assoc,String input_data)
	{		
			this.cache[Index_bits][invalid_index]=Tag_bits+"   ";
			for(int i=0;i<assoc;i++)
			{
				if(i!=invalid_index)
				{
					this.counter[Index_bits][i]++;
				}
			}
			this.counter[Index_bits][invalid_index]=0;
			this.valid_bits[Index_bits][invalid_index]=1;
			if(call==false && count!=0)
			{
				call=true;
				count=0;
			}
	}
	public void Miss(int Index_bits,String Tag_bits,int assoc,String input_data)
	{
		if(call==false && count!=0)
		{
			int index=0;
			int max=this.counter[Index_bits][0];
			for(int i=0;i<assoc;i++)
			{
				if(this.counter[Index_bits][i]>max)
				{
					max=this.counter[Index_bits][i];
					index=i;
				}
			}
			boolean ret=this.cache[Index_bits][index].endsWith("D ");
			if (ret==true)
				Writeback_L2++;
			this.cache[Index_bits][index]=Tag_bits+"   ";
			for(int i=0;i<assoc;i++)
			{
				if(i!=index)
				{
					this.counter[Index_bits][i]++;
				}
			}	
			this.counter[Index_bits][index]=0;
			count=0;
			call=true;
		}
		else
		{
			Hit_VC=false;
			int index=0;
			int max=this.counter[Index_bits][0];
			for(int i=0;i<assoc;i++)
			{
				if(this.counter[Index_bits][i]>max)
				{
					max=this.counter[Index_bits][i];
					index=i;
				}
			}
			String data_VC=this.cache[Index_bits][index];
			if(set==1)
			{
				Swap_request++;
				flag=false;
				VC.Read_cache_VC(data_VC,input_data,Index_bits);
			}
			if (set==1 && Hit_VC==false)
			{	
				VC.Write_cache_VC(data_VC,input_data,Index_bits);
			} 
			if (set!=1)
			{
				String[] tag= this.cache[Index_bits][index].split(" ");
				String data=this.regenerate(tag[0]);
				data="  "+data;
				boolean ret=this.cache[Index_bits][index].endsWith("D ");
				if(ret==true && sel==1)
				{	Write_countL2++;
					select=0;
					call=false;
					count++;
					L2.Write_cache(data,L2_assoc);
				}
				if (ret==true)
					Writeback_count++;
			}
			this.cache[Index_bits][index]=Tag_bits;
			if (flag==true )
			{
				this.cache[Index_bits][index]+=" D ";
			}
			else
			{
				this.cache[Index_bits][index]+="   ";
			}
			for(int i=0;i<assoc;i++)
			{
				if(i!=index)
				{
					this.counter[Index_bits][i]++;
				}
			}	
			this.counter[Index_bits][index]=0;
			call=false;
			count=0;
		}
	}
	public void Invalid_write(int Index_bits,int invalid_index,String Tag_bits,int assoc,String input_data)
	{
		this.cache[Index_bits][invalid_index]=Tag_bits;
		this.cache[Index_bits][invalid_index]+=" D ";
		for(int i=0;i<assoc;i++)
		{
			if(i!=invalid_index)
			{
				this.counter[Index_bits][i]++;
			}
		}
		this.counter[Index_bits][invalid_index]=0;
		this.valid_bits[Index_bits][invalid_index]=1;
		if(call==false && count!=0)
		{
			call=true;
			count=0;
		}
	}
	public void Miss_write(int Index_bits,String Tag_bits,int assoc,String input_data)
	{
		if(call==false && count!=0)
		{
			int index=0;
			int max=this.counter[Index_bits][0];
			for(int i=0;i<assoc;i++)
			{
				if(this.counter[Index_bits][i]>max)
				{
					max=this.counter[Index_bits][i];
					index=i;
				}
			}	
			boolean ret=this.cache[Index_bits][index].endsWith("D ");
			if (ret==true)
				Writeback_L2++;
			this.cache[Index_bits][index]=Tag_bits;
			this.cache[Index_bits][index]+=" D ";
			for(int i=0;i<assoc;i++)
			{
				if(i!=index)
				{
					this.counter[Index_bits][i]++;
				}
			}	
			this.counter[Index_bits][index]=0;
			call=true;
			count=0;
		}
		else
		{
			Hit_VC=false;
			int index=0;
			int max=this.counter[Index_bits][0];
			for(int i=0;i<assoc;i++)
			{
				if(this.counter[Index_bits][i]>max)
				{
					max=this.counter[Index_bits][i];
					index=i;
				}
			}	
			String data_VC=this.cache[Index_bits][index];
			if(set==1)
			{
				Swap_request++;
				VC.Read_cache_VC(data_VC,input_data,Index_bits);
			}
			if (set==1 && Hit_VC==false)
			{	
				VC.Write_cache_VC(data_VC,input_data,Index_bits);
			}
			if (set!=1)
			{
				String[] tag= this.cache[Index_bits][index].split(" ");
				String data=this.regenerate(tag[0]);
				data="  "+data;
				boolean ret=this.cache[Index_bits][index].endsWith("D ");
				if(ret==true && sel==1)
				{	Write_countL2++;
					select=0;
					call=false;
					count++;
					L2.Write_cache(data,L2_assoc);
				}
				if (ret==true)
					Writeback_count++;
			}	
			this.cache[Index_bits][index]=Tag_bits;
			this.cache[Index_bits][index]+=" D ";
			for(int i=0;i<assoc;i++)
			{
				if(i!=index)
				{
					this.counter[Index_bits][i]++;
				}
			}	
			this.counter[Index_bits][index]=0;
			call=false;
			count=0;
			select=1;
		}	
		
	}
	public void generate(String input_data)
	{
		int address_bits;
		String Index_number,Tag_number;
		String address = null;
		
		chars=input_data.charAt(0);
		address=input_data.substring(2,input_data.length());
		address_bits=Integer.parseInt(address,16);
		String binary=Integer.toBinaryString(address_bits);
		int len=(32-binary.length());
		switch(len)
		{
			case 1:for(int z=0;z<len;z++)
					{
						binary="0"+binary;
					}
					break;
			case 2:for(int z=0;z<len;z++)
					{
						binary="0"+binary;
					}
					break;
			case 3:for(int z=0;z<len;z++)
					{
						binary="0"+binary;
					}
					break;
			case 4:for(int z=0;z<len;z++)
					{
						binary="0"+binary;
					}
					break;	
			case 5:for(int z=0;z<len;z++)
					{
						binary="0"+binary;
					}
					break;
			case 6:for(int z=0;z<len;z++)
					{
						binary="0"+binary;
					}
					break;
			case 7:for(int z=0;z<len;z++)
					{
						binary="0"+binary;
					}
					break;
			case 8:for(int z=0;z<len;z++)
					{
						binary="0"+binary;
					}
					break;	
			case 9:for(int z=0;z<len;z++)
					{
						binary="0"+binary;
					}	
					break;	
			case 10:for(int z=0;z<len;z++)
					{
						binary="0"+binary;
					}
					break;	
			case 11:for(int z=0;z<len;z++)
					{
						binary="0"+binary;
					}
					break;	
			case 12:for(int z=0;z<len;z++)
					{
						binary="0"+binary;
					}
					break;	
			default:break;					
		}
		if (this.vc!=1)
		{
			Index_number=binary.substring((int)(32-(this.Num_block_offset_bits+this.Num_index_bits)),(int)(32-(this.Num_block_offset_bits+this.Num_index_bits)+this.Num_index_bits));
			Index_bits=Integer.parseInt(Index_number,2);
		}
		Tag_number=binary.substring(0,(int)(32-(this.Num_block_offset_bits+this.Num_index_bits)));
		int hexnum=Integer.parseInt(Tag_number,2);
		Tag_bits=Integer.toHexString(hexnum);
				
	}
	public void Read_cache_VC(String data,String input_data,int Index_bits)
	{
		VC.generate(input_data);
		String[] tag=data.split(" ");
		int hex=Integer.parseInt(tag[0],16);
		String binary=Integer.toBinaryString(hex);
		String Index_b=Integer.toBinaryString(Index_bits);
		double len=Index_b.length();
		for(double j=0;j<(L1.Num_index_bits-len);j++)
		{
			Index_b="0"+Index_b;
		}
		String temp=binary+Index_b;
		for(int i=0;i<L1.Num_block_offset_bits;i++)
		{
			temp=temp+"0";
		}		
		int data1=Integer.parseInt(temp,2);
		String data2=Integer.toHexString(data1);
		data2="  "+data2;
		boolean ret=data.endsWith("D ");
		for(int i=0;i<Vc_num_blocks;i++)
		{	
			if (this.cache[this.Index_bits][i]!=null) 
			{
				String[] sub= this.cache[this.Index_bits][i].split(" ");
				if(sub[0].compareTo(this.Tag_bits)==0)
				{	
					for(int j=0;j<Vc_num_blocks;j++)
					{
						if(this.counter[this.Index_bits][i]>this.counter[this.Index_bits][j])
						{
							this.counter[this.Index_bits][j]++;
						}
					}
					this.counter[this.Index_bits][i]=0;
					flag=this.cache[this.Index_bits][i].endsWith("D ");
					int index=this.Index_bits;
					VC.generate(data2);
					this.cache[index][i]=this.Tag_bits;
					if (ret==true)
					{
						this.cache[index][i]+=" D ";
					}
					else
						this.cache[index][i]+="   ";
					Hit_VC=true;
					Swap_count++;
				}
			}	
		}
	}
	public void Write_cache_VC(String data,String input_data,int Index_bits)
	{	
		int index=0;
		VC.generate(input_data);
		int max=this.counter[this.Index_bits][0];
		for(int i=0;i<Vc_num_blocks;i++)
		{
			if(this.counter[this.Index_bits][i]>max)
			{
				max=this.counter[this.Index_bits][i];
				index=i;
			}
		}		
		if (this.cache[this.Index_bits][index]!=null) 
		{
			String[] tag= this.cache[this.Index_bits][index].split(" ");
			String data2=this.regenerate(tag[0]);
			data2="w "+data2;
			boolean ret=this.cache[this.Index_bits][index].endsWith("D ");
			if(ret==true && sel==1)
			{	Write_countL2++;
				select=0;
				call=false;
				count++;
				L2.Write_cache(data2,L2_assoc);
			}
			if (ret==true)
			{			
				Writeback_count++;
			}
		}
		int index2=this.Index_bits;
		String[] tag2= data.split(" ");
		int hex=Integer.parseInt(tag2[0],16);
		String binary=Integer.toBinaryString(hex);
		String Index_b=Integer.toBinaryString(Index_bits);
		double len=Index_b.length();
		for(double j=0;j<(L1.Num_index_bits-len);j++)
		{
			Index_b="0"+Index_b;
		}
		String temp=binary+Index_b;
		for(int i=0;i<L1.Num_block_offset_bits;i++)
		{
			temp=temp+"0";
		}			
		int data1=Integer.parseInt(temp,2);
		String data3=Integer.toHexString(data1);
		boolean ret2=data.endsWith("D ");
		data3="  "+data3;
		this.generate(data3);
		this.cache[index2][index]=this.Tag_bits;
		if (ret2==true)
			this.cache[index2][index]+=" D ";
		else
			this.cache[index2][index]+="   ";
		for(int i=0;i<Vc_num_blocks;i++)
		{
			if(i!=index)
			{
				this.counter[index2][i]++;
			}
		}	
		this.counter[index2][index]=0;
		call=false;
		count=0;
	}
	public String regenerate(String data)
	{
		String temp;
		int hex=Integer.parseInt(data,16);
		String binary=Integer.toBinaryString(hex);
		if (this.vc!=1)
		{String Index_b=Integer.toBinaryString(this.Index_bits);
		double len=Index_b.length();
		for(double j=0;j<(this.Num_index_bits-len);j++)
		{
			Index_b="0"+Index_b;
		}
			temp=binary+Index_b;
		}
		else
		{
			temp=binary;
		}
		for(int i=0;i<this.Num_block_offset_bits;i++)
		{
			temp=temp+"0";
		}			
		int data1=Integer.parseInt(temp,2);
		String data2=Integer.toHexString(data1);
		return data2;
	}
	public static void display(int L1_assoc)
	{	
		DecimalFormat df = new DecimalFormat("####.####");
		int i;
		for(i=0;i<5;i++)
		{
			System.out.print("=");
		}	
		System.out.print(" L1 contents ");
		for(i=0;i<5;i++)
		{
			System.out.print("=");
		}	
		System.out.println(" ");
		for(i=0;i<L1.sets;i++)
		{	String temp;
			int temp2;
			for (int j=0;j<L1_assoc;j++)
			{
				for(int z=j+1;z<L1_assoc;z++)
				{
					if(L1.counter[i][j]>L1.counter[i][z])
					{
						temp=L1.cache[i][j];
						L1.cache[i][j]=L1.cache[i][z];
						L1.cache[i][z]=temp;
						temp2=L1.counter[i][j];
						L1.counter[i][j]=L1.counter[i][z];
						L1.counter[i][z]=temp2;
					}
				}	
			}
	
		}	
		for(i=0;i<L1.sets;i++)
		{
			if(i<10)
				System.out.print("set   "+i+":   ");
			else if(i<100)
				System.out.print("set  "+i+":   ");
			else
				System.out.print("set "+i+":   ");
			for(int j=0;j<L1_assoc;j++)
			{
				System.out.print(L1.cache[i][j]+" ");
				
			}
			System.out.println("");
		}
		if (set==1)
		{	
			System.out.println(" ");
			for(i=0;i<5;i++)
			{
				System.out.print("=");
			}	
			System.out.print(" VC contents ");
			for(i=0;i<5;i++)
			{
				System.out.print("=");
			}	
			System.out.println(" ");
			for(i=0;i<VC.sets;i++)
			{	String temp1;
				int temp2; 
				for (int j=0;j<Vc_num_blocks;j++)
				{
					for(int z=j+1;z<Vc_num_blocks;z++)
					{
						//System.out.println(L2_assoc);
						if(VC.counter[i][j]>VC.counter[i][z])
						{
							temp1=VC.cache[i][j];
							VC.cache[i][j]=VC.cache[i][z];
							VC.cache[i][z]=temp1;
							temp2=VC.counter[i][j];
							VC.counter[i][j]=VC.counter[i][z];
							VC.counter[i][z]=temp2;
						}
					}	
				}
			}
			for(i=0;i<VC.sets;i++)
			{
			if(i<10)
				System.out.print("set   "+i+":   ");
			else 
				System.out.print("set  "+i+":   ");
			for(int j=0;j<Vc_num_blocks;j++)
			{
				System.out.print(VC.cache[i][j]+" ");
			}
			System.out.println("");
			}
			
		}
		if (sel==1)
		{	
			System.out.println(" ");
			for(i=0;i<5;i++)
			{
				System.out.print("=");
			}	
			System.out.print(" L2 contents ");
			for(i=0;i<5;i++)
			{
				System.out.print("=");
			}	
			System.out.println(" ");
			for(i=0;i<L2.sets;i++)
			{	
				String temp;
				int temp2;
				for (int j=0;j<L2_assoc;j++)
				{
					for(int z=j+1;z<L2_assoc;z++)
					{
						//System.out.println(L2_assoc);
						if(L2.counter[i][j]>L2.counter[i][z])
						{
							temp=L2.cache[i][j];
							L2.cache[i][j]=L2.cache[i][z];
							L2.cache[i][z]=temp;
							temp2=L2.counter[i][j];
							L2.counter[i][j]=L2.counter[i][z];
							L2.counter[i][z]=temp2;
						}
					}	
				}
	
			}
			for(i=0;i<L2.sets;i++)
			{
				if(i<10)
					System.out.print("set   "+i+":   ");
				else if(i<100)
					System.out.print("set  "+i+":   ");
				else
					System.out.print("set "+i+":   ");
				for(int j=0;j<L2_assoc;j++)
				{
					System.out.print(L2.cache[i][j]+" ");
					
				}
				System.out.println("");
			}
		}
		System.out.println(" ");
		for(i=0;i<5;i++)
		{
			System.out.print("=");
		}	
		System.out.print(" Simulation results ");
		for(i=0;i<5;i++)
		{
			System.out.print("=");
		}	
		System.out.println(" ");
		System.out.println(" a.number of L1 reads:\t\t\t"+Read_countL1);
		System.out.println(" b.number of L1 read misses:\t\t"+Read_Miss_countL1);
		System.out.println(" c.number of L1 writes:\t\t\t"+Write_countL1);
		System.out.println(" d.number of L1 write misses:\t\t"+Write_miss_countL1);	
		System.out.println(" e.number of swap requests:\t\t"+Swap_request);
		System.out.println(" f.swap request rate:\t\t\t"+df.format((double)(Swap_request)/(Write_countL1+Read_countL1)));
		System.out.println(" g.number of swaps:\t\t\t"+Swap_count);
		System.out.println(" h.combined L1+VC miss rate:\t\t"+df.format((double)(Read_Miss_countL1+Write_miss_countL1-Swap_count)/(Write_countL1+Read_countL1)));
		System.out.println(" i.number writebacks from L1/VC:\t"+Writeback_count);
		System.out.println(" j.number of L2 reads:\t\t\t"+Read_countL2);
		System.out.println(" k.number of L2 read misses:\t\t"+Read_Miss_countL2);
		System.out.println(" l.number of L2 writes:\t\t\t"+Write_countL2);
		System.out.println(" m.number of L2 write misses:\t\t"+Write_miss_countL2);	
		if (sel==1)
			System.out.println(" n.L2 miss rate:\t\t\t"+df.format((double)Read_Miss_countL2/Read_countL2));
		else
			System.out.println(" n.L2 miss rate:\t\t\t0");
		System.out.println(" o.number of writebacks from L2:\t"+Writeback_L2);
		if (sel==1)
		{
			Memory_traffic=Read_Miss_countL2+Write_miss_countL2+Writeback_L2;
		}
		else
		{
			Memory_traffic=Read_Miss_countL1+Write_miss_countL1-Swap_count+Writeback_count;
		}
		System.out.println(" p.total memory traffic:\t\t"+Memory_traffic);
		System.out.println(" ");
	}
	public static void Config_display(int Blocksize,int L1_size,int L1_assoc,int L2_size,String trace_file)
	{
		int i;
		for(i=0;i<5;i++)
		{
			System.out.print("=");
		}	
		System.out.print(" Simulator configuration ");
		for(i=0;i<5;i++)
		{
			System.out.print("=");
		}	
		System.out.println(" ");
		System.out.print(" BLOCKSIZE:\t\t"+Blocksize);
		System.out.println(" ");
		System.out.print(" L1_SIZE:\t\t"+L1_size);
		System.out.println(" ");
		System.out.print(" L1_ASSOC:\t\t"+L1_assoc);
		System.out.println(" ");
		System.out.print(" VC_NUM_BLOCKS:\t\t"+Vc_num_blocks);
		System.out.println(" ");
		System.out.print(" L2_SIZE:\t\t"+L2_size);
		System.out.println(" ");
		System.out.print(" L2_ASSOC:\t\t"+L2_assoc);
		System.out.println(" ");
		System.out.print(" trace_file:\t\t"+trace_file);
		System.out.println(" ");
		System.out.println(" ");
	}
	
}
