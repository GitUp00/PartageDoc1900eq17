// /*
//  * Nom Tp2P2.cpp
//  * Auteur : Gagné S. et Sénécal J-N
//  * Date : 14 septembre 2018
//  * Machine a etat probleme 2 tp2
//  */

// /*
//  * Pin de la LED connecte au PORTA(pin 1 et pin 2)

#include <avr/interrupt.h>
#include <avr/io.h>

#define F_CPU 8000000
#include <util/delay.h>
volatile uint8_t minuterieExpiree = 0; //-----p2
volatile uint8_t boutonPoussoir = 0;   //-------p2


void compter(uint8_t a)
    {
		for (int i = 0; i < a; i++)
		{
		compteur++;
		_delay_ms(100);
		}
		
		compteur
	}
	/*
	void compter2 (uint8_t b)
	{
		for
	}
	*/

int main()
{
	/*
	class Memoire24 :: 
	{
	ecriture(0x00, "*P*O*L*Y*T*E*C*H*N*I*Q*U*E**M*O*N*T*R*E*A*L*");
	_delay_ms(3000);
	lecture(0x00, ecriture);
	
	if (ecriture ==lecture)
	{
	PORTA = 0b01;	
	}
	if(ecriture !=lecture)
	{
	PORTA = 0b10;	
	}
	lecture
    DDRA = 0xff;
    DDRB = 0xff; // PORT B est en mode sortie
    DDRC = 0xff; // PORT C est en mode sortie
    DDRD = 0x00; // PORT D est en mode entree
    */
    
    uint8_t compteur = 0;
    while(compteur !=120)
    {
		compter(120);
	}
	PORTA = 0b01;
	_delay_ms(500);
	PORTA = 0b00;
	_delay_ms(2000);
	PORTA = 0b10;
	
	//
	
	//
	
	PORTA = 0b01;
	_delay_ms(1000);
	PORTA=0b00;
	compteur = 0;
}


    
    return 0;
}
